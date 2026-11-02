package io.github.genesjc.opensourcepanda.data

import io.github.genesjc.opensourcepanda.domain.addIssuesToRepoGroups
import io.github.genesjc.opensourcepanda.domain.canRequestNextPage
import io.github.genesjc.opensourcepanda.domain.groupByRepo
import io.github.genesjc.opensourcepanda.domain.resolveActiveMode
import io.github.genesjc.opensourcepanda.domain.shouldSwitchMode
import io.github.genesjc.opensourcepanda.domain.updateIssuesInRepoGroups
import io.github.genesjc.opensourcepanda.model.IssueLabelFilter
import io.github.genesjc.opensourcepanda.model.LanguageFilter
import io.github.genesjc.opensourcepanda.model.RateLimitInfo
import io.github.genesjc.opensourcepanda.model.RepoIssueGroup
import io.github.genesjc.opensourcepanda.model.SearchFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The Top Repos list state and the logic that changes it. TopReposViewModel maps it to what
 * the screen shows and forwards the user's actions here.
 *
 * The list comes from one Search API call per load for the active mode's label, grouped by
 * parent repo and ranked by match count. loadMore() adds the next page into the groups already
 * held, because a repo's matches can span pages and appending would show it twice; a rank can
 * change after a Load More, by design. A Load More that hits the Search API rate limit emits one
 * RateLimited event and leaves paging untouched, so the button works again after the reset.
 * load() is cache-first for the (language, mode) pair, restoring the page and canLoadMore too;
 * fresh data is never automatic. checkForNewRepos() updates the list from page 1 without
 * replacing it (a replacing refresh once threw away every Load More).
 *
 * An app-wide singleton so the state outlives a composition: open() starts a fresh visit and
 * cancels leftover work. It runs in the shared [AppScope]; the Koin module builds the instance.
 * More in docs/design-notes.md, "Top Repos state".
 */
class TopReposStateHolder(
    private val modeRepository: IssueModeRepository,
    private val tokenRepository: ApiTokenRepository,
    private val cacheRepository: RepoIssueGroupRepository,
    private val searchRepository: IssueSearchRepository,
    private val scope: CoroutineScope = AppScope.scope,
) {

    private val _state = MutableStateFlow(TopReposState())
    val state: StateFlow<TopReposState> = _state.asStateFlow()

    private val eventChannel = Channel<TopReposEvent>(Channel.BUFFERED)
    val events: Flow<TopReposEvent> = eventChannel.receiveAsFlow()

    /** The active Issue Mode, or null while none is chosen (which DiscoverNavHost prevents from ever being observed here). */
    val mode: StateFlow<IssueLabelFilter?> = modeRepository.mode

    /** The Search API rate-limit numbers for the chip, or null until they are known. */
    val rateLimit: StateFlow<RateLimitInfo?> = searchRepository.rateLimit

    /** Whether a token is in use right now. */
    val hasToken: StateFlow<Boolean> = tokenRepository.effectiveToken
        .map { it.isNotBlank() }
        .stateIn(scope, SharingStarted.Eagerly, tokenRepository.effectiveToken.value.isNotBlank())

    /** Guards checkForNewRepos() against overlapping calls. */
    private var isCheckingForNewRepos = false

    /** Parent of everything one visit to the screen starts; open() cancels it and starts a new one. */
    private var visitJob: Job = SupervisorJob()

    init {
        // When the token changes while the list is stuck on a token problem, clear it and retry with
        // the new token. drop(1) skips the value already held. A change while the list works is ignored;
        // the next request just uses the new token.
        scope.launch {
            tokenRepository.effectiveToken.drop(1).collect {
                // The old token's rate-limit numbers don't describe the
                // new one; drop them and look the new ones up.
                searchRepository.clearRateLimit()
                refreshRateLimitIfUnknown()
                if (_state.value.needsTokenFix) {
                    _state.update { it.copy(tokenRejected = false, loadFailure = null) }
                    load(_state.value.selectedLanguage)
                }
            }
        }
        // Adding or removing a token switches between demo and real data, so what is listed is reloaded
        // (from the cache of the data now in use, else from its source).
        scope.launch {
            tokenRepository.activeTag.drop(1).collect {
                if (!_state.value.needsTokenFix) load(_state.value.selectedLanguage)
            }
        }
    }

    /**
     * Starts a fresh visit: cancels leftover work, clears the state and any undelivered events,
     * and loads (cache first).
     */
    fun open() {
        visitJob.cancel()
        visitJob = SupervisorJob()
        _state.value = TopReposState()
        while (eventChannel.tryReceive().isSuccess) {
            // Drain events nobody collected from the previous visit.
        }
        load(_state.value.selectedLanguage)
    }

    fun selectLanguage(language: LanguageFilter) {
        _state.update { it.copy(selectedLanguage = language) }
        load(language)
    }

    /** Switches the Issue Mode in place and reloads; a no-op if that mode is already active. */
    fun selectMode(mode: IssueLabelFilter) {
        if (!shouldSwitchMode(modeRepository.mode.value, mode)) return
        modeRepository.setMode(mode)
        load(_state.value.selectedLanguage)
    }

    /** Retries the INITIAL load — for when the list is empty because the first fetch failed. Unrelated to retryLoadMore(), which retries a failed Load More instead. */
    fun retry() = load(_state.value.selectedLanguage)

    /**
     * Adds the next page to what is held. A no-op while one is in flight or canLoadMore is false,
     * so it can be wired straight to a button.
     */
    fun loadMore() {
        val current = _state.value
        if (current.isLoadingMore || !current.canLoadMore) return
        fetchPage(
            language = current.selectedLanguage,
            page = current.currentPage + 1,
            isLoadMore = true,
        )
    }

    /** Retries a Load More that failed — distinct from retry(), which is for the initial load. Clears the failure and re-attempts the SAME next page, not a fresh load. */
    fun retryLoadMore() {
        _state.update { it.copy(loadMoreFailure = null) }
        loadMore()
    }

    /**
     * Fetches page 1 and updates the held groups instead of replacing them: new repos are added,
     * existing ones gain new matches, and known issues are refreshed. currentPage and canLoadMore
     * are left alone, so this never resets how far Load More has got.
     */
    fun checkForNewRepos() {
        if (isCheckingForNewRepos) return
        val activeMode = resolveActiveMode(modeRepository.mode.value)
        val snapshot = _state.value
        val language = snapshot.selectedLanguage

        launchInVisit {
            isCheckingForNewRepos = true
            try {
                when (val result = searchRepository.searchTopRepos(activeMode, language, page = 1)) {
                    is IssueSearchResult.Success -> {
                        val fetchedIssues = result.page.issues
                        val totalCount = result.page.totalCount

                        // Captured out of update{} (which may retry) so the values persisted are the ones committed;
                        // snapshotting before the suspend call would overwrite a concurrent Load More's newer page.
                        // `repoGroups` is the new list; `current.repoGroups` is the one held before.
                        var repoGroups: List<RepoIssueGroup> = emptyList()
                        var newRepoCount = 0
                        var newIssueCount = 0
                        var currentPageAtCommit = snapshot.currentPage
                        var canLoadMoreAtCommit = snapshot.canLoadMore
                        var previousUpdatedAtMs = snapshot.lastUpdatedAtMs
                        val fetchedAtMs = System.currentTimeMillis()
                        _state.update { current ->
                            val update = updateIssuesInRepoGroups(current.repoGroups, fetchedIssues)
                            repoGroups = update.repoGroups
                            newRepoCount = update.newRepoCount
                            newIssueCount = update.newIssueCount
                            currentPageAtCommit = current.currentPage
                            canLoadMoreAtCommit = current.canLoadMore
                            previousUpdatedAtMs = current.lastUpdatedAtMs
                            current.copy(
                                repoGroups = repoGroups,
                                totalIssueCount = totalCount,
                                lastUpdatedAtMs = fetchedAtMs,
                            )
                        }
                        cacheRepository.save(
                            language = language,
                            mode = activeMode,
                            repoGroups = repoGroups,
                            totalIssueCount = totalCount,
                            currentPage = currentPageAtCommit,
                            canLoadMore = canLoadMoreAtCommit,
                        )
                        eventChannel.trySend(
                            TopReposEvent.NewReposChecked(newRepoCount, newIssueCount, previousUpdatedAtMs, fetchedAtMs),
                        )
                    }
                    is IssueSearchResult.HttpError -> eventChannel.trySend(TopReposEvent.CheckFailedHttp(result.code))
                    is IssueSearchResult.Failed -> eventChannel.trySend(TopReposEvent.CheckFailed(result.message))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                eventChannel.trySend(TopReposEvent.CheckFailed(e.message))
            } finally {
                isCheckingForNewRepos = false
            }
        }
    }

    /** Clears the entire persistent cache, then reloads so nothing is shown that the cache no longer has. */
    fun clearCache() {
        launchInVisit {
            cacheRepository.clearAll()
            load(_state.value.selectedLanguage)
        }
    }

    /**
     * Re-reads the cache for the current (language, mode) and adopts it. Called when this screen is
     * navigated back to, so a repo refreshed on the repo screen shows its new match count (or
     * disappears) with no network call. The two state holders have no handle on each other: the repo
     * screen writes to the cache and this pulls the change back. A no-op if nothing is cached.
     */
    fun syncFromCache() {
        val activeMode = resolveActiveMode(modeRepository.mode.value)
        val language = _state.value.selectedLanguage
        launchInVisit {
            val cached = cacheRepository.load(language, activeMode) ?: return@launchInVisit
            _state.update {
                it.copy(
                    repoGroups = cached.repoGroups,
                    totalIssueCount = cached.totalIssueCount,
                    currentPage = cached.currentPage,
                    canLoadMore = cached.canLoadMore,
                    lastUpdatedAtMs = cached.fetchedAtMs,
                )
            }
        }
    }

    private fun load(language: LanguageFilter) {
        // Defensive fallback only: DiscoverNavHost never shows this screen while the mode is null.
        val activeMode = resolveActiveMode(modeRepository.mode.value)

        // Fresh load: reset the pagination errors but leave repoGroups as is until new data arrives,
        // so a language switch does not blank the screen behind a spinner.
        _state.update {
            it.copy(
                currentPage = 0,
                canLoadMore = true,
                loadMoreFailure = null,
            )
        }

        launchInVisit {
            _state.update { it.copy(isLoading = true, loadFailure = null) }
            val cached = cacheRepository.load(language, activeMode)
            if (cached != null) {
                _state.update {
                    it.copy(
                        repoGroups = cached.repoGroups,
                        totalIssueCount = cached.totalIssueCount,
                        currentPage = cached.currentPage,
                        canLoadMore = cached.canLoadMore,
                        lastUpdatedAtMs = cached.fetchedAtMs,
                        isLoading = false,
                    )
                }
                // A cache hit never touches the network, so nothing has
                // told the search repository the rate limit yet this session.
                refreshRateLimitIfUnknown()
            } else {
                fetchPage(language = language, page = 1, isLoadMore = false)
            }
        }
    }

    /** Fills in the rate-limit chip after a cache-hit load, so it shows real numbers with the first list. */
    private fun refreshRateLimitIfUnknown() {
        launchInVisit { searchRepository.loadRateLimitIfUnknown() }
    }

    private fun fetchPage(language: LanguageFilter, page: Int, isLoadMore: Boolean) {
        // Defensive fallback only — see load().
        val activeMode = resolveActiveMode(modeRepository.mode.value)

        launchInVisit {
            if (isLoadMore) {
                _state.update { it.copy(isLoadingMore = true, loadMoreFailure = null) }
            } else {
                _state.update { it.copy(isLoading = true, loadFailure = null) }
            }
            try {
                when (val result = searchRepository.searchTopRepos(activeMode, language, page)) {
                    is IssueSearchResult.Success -> {
                        val newIssues = result.page.issues
                        val totalCount = result.page.totalCount

                        // Computed inside update{} (which may retry) and captured, so the values persisted are the ones
                        // committed to state. `repoGroups` is the new list; `state.repoGroups` is the one held before.
                        var repoGroups: List<RepoIssueGroup> = emptyList()
                        var newCanLoadMore = false
                        val fetchedAtMs = System.currentTimeMillis()
                        _state.update { state ->
                            repoGroups = if (isLoadMore) {
                                addIssuesToRepoGroups(state.repoGroups, newIssues)
                            } else {
                                groupByRepo(newIssues)
                            }
                            newCanLoadMore = canRequestNextPage(fetchedThisPage = newIssues.size, justFetchedPage = page)
                            state.copy(
                                repoGroups = repoGroups,
                                isLoading = false,
                                isLoadingMore = false,
                                totalIssueCount = totalCount,
                                currentPage = page,
                                canLoadMore = newCanLoadMore,
                                lastUpdatedAtMs = fetchedAtMs,
                                tokenRejected = false,
                            )
                        }
                        cacheRepository.save(
                            language = language,
                            mode = activeMode,
                            repoGroups = repoGroups,
                            totalIssueCount = totalCount,
                            currentPage = page,
                            canLoadMore = newCanLoadMore,
                        )
                    }
                    is IssueSearchResult.HttpError -> {
                        if (isLoadMore && result.rateLimited) {
                            // Leave canLoadMore and currentPage as they are; only the footer's loading state clears.
                            _state.update { it.copy(isLoadingMore = false) }
                            eventChannel.trySend(TopReposEvent.RateLimited)
                            return@launchInVisit
                        }
                        // 401 means GitHub rejected the configured token. Surfaced as tokenRejected so the chip can tell
                        // "no token" from "a token that does not work".
                        val failure = SearchFailure.Http(result.code)
                        val isTokenRejected = result.code == 401
                        _state.update {
                            if (isLoadMore) {
                                it.copy(isLoadingMore = false, loadMoreFailure = failure)
                            } else {
                                it.copy(isLoading = false, loadFailure = failure, tokenRejected = isTokenRejected)
                            }
                        }
                    }
                    is IssueSearchResult.Failed -> showLoadFailure(isLoadMore, result.message)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showLoadFailure(isLoadMore, e.message)
            }
        }
    }

    /** A load or Load More that failed before GitHub answered: held for the list (the initial load) or the footer (Load More). */
    private fun showLoadFailure(isLoadMore: Boolean, detail: String?) {
        val failure = SearchFailure.Failed(detail)
        _state.update {
            if (isLoadMore) {
                it.copy(isLoadingMore = false, loadMoreFailure = failure)
            } else {
                it.copy(isLoading = false, loadFailure = failure)
            }
        }
    }

    private fun launchInVisit(block: suspend () -> Unit) {
        scope.launch(visitJob) { block() }
    }
}
