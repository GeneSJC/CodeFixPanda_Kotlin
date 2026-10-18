package com.example.githubissuefinder.data

import com.example.githubissuefinder.model.IssueLabelFilter
import com.example.githubissuefinder.model.LanguageFilter
import com.example.githubissuefinder.model.RepoIssueGroup
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
 * The Top Repos data state: everything the screen's list is built from,
 * and nothing about how it is shown. TopReposStateHolder is the one place it
 * lives and changes; TopReposViewModel maps it to TopReposUiState (which
 * adds the message text).
 */
data class TopReposState(
    /** The repo groups shown, ranked by match count; see RepoIssueGroup. */
    val repoGroups: List<RepoIssueGroup> = emptyList(),
    val selectedLanguage: LanguageFilter = LanguageFilter.KOTLIN,
    /** Total matching ISSUES GitHub reports for the current query (not a repo count). */
    val totalIssueCount: Int = 0,
    /** Which page of the current query has been fetched so far; reset on every fresh load. */
    val currentPage: Int = 1,
    /** False once there is nothing further to fetch (a short page, or GitHub's 1,000-result cap). */
    val canLoadMore: Boolean = true,
    /** When the shown repoGroups were fetched: a live call, or the cache entry's own stamp. Null before the first load resolves. */
    val lastUpdatedAtMs: Long? = null,
    /** True while the initial load (the full-screen one) is in flight. */
    val isLoading: Boolean = false,
    /** True while a Load More is in flight, kept separate from [isLoading] so the visible list never disappears behind a spinner. */
    val isLoadingMore: Boolean = false,
    /** True when no GitHub token is configured at all. */
    val noTokenConfigured: Boolean = false,
    /** True when a token is configured but GitHub rejected it (HTTP 401). */
    val tokenRejected: Boolean = false,
    /** Set when the whole list failed to load. */
    val loadFailure: SearchFailure? = null,
    /** Set when a Load More failed; the list itself is left exactly as it was. */
    val loadMoreFailure: SearchFailure? = null,
) {
    /** True while the list cannot load because of the token: none is set, or GitHub rejected the one in use. */
    val needsTokenFix: Boolean
        get() = noTokenConfigured || tokenRejected
}

/** One-shot things the screen should tell the user, reported as facts. The ViewModel turns them into the Snackbar text. */
sealed interface TopReposEvent {
    /** A Load More hit GitHub's search rate limit. */
    data object RateLimited : TopReposEvent

    /** Check for New Repos finished; [previousUpdatedAtMs] is the list's stamp before this check (null if it had none). */
    data class NewReposChecked(
        val newRepoCount: Int,
        val newIssueCount: Int,
        val previousUpdatedAtMs: Long?,
        val checkedAtMs: Long,
    ) : TopReposEvent

    /** Check for New Repos got an error status from GitHub. */
    data class CheckFailedHttp(val code: Int) : TopReposEvent

    /** Check for New Repos failed before GitHub answered. */
    data class CheckFailed(val message: String?) : TopReposEvent
}

/**
 * The state holder for the Top Repos list: the one place the list's
 * data state lives, and the owner of all the logic that changes it.
 * TopReposViewModel does not hold or change this state; it maps it to
 * what the screen shows and forwards the user's actions here.
 *
 * (The long design history of how this list is built, formerly the
 * ViewModel's class comment, is kept below; it is about this logic, so
 * it moved with it.)
 *
 * v0.42 — THE DATA-LAYER SWAP. The list is built from GitHub's Issue
 * Search API queried directly for the active mode's label, then GROUPED
 * by parent repo (repository_url, via Issue.repoFullName). Each group
 * becomes one RepoIssueGroup. Cost: ONE Search API call per load. A repo
 * only appears at all if it had a matching issue in the batch, so a repo
 * with zero matching issues literally cannot be shown. Ranking is
 * match-count only (RepoIssueGroup.matchCount, descending); richer
 * per-repo enrichment remains an optional follow-up. The old 60-second
 * cooldown is gone for the same reason: one call per load no longer
 * risks the 30/min Search API cap.
 *
 * v0.43 (PAGINATION + CROSS-PAGE DEDUP): loadMore() fetches the next
 * page and the result is ADDED to the existing repoGroups, not appended
 * as new cards. A repo's matching issues are not guaranteed to land on
 * one page: if "Apps Android Commons" has 5 matches and 3 landed on page
 * 1, the other 2 can land on page 2, and naively appending would show it
 * as a SECOND card. addIssuesToRepoGroups() (data/RepoIssueGrouping.kt)
 * prevents that by re-grouping new issues against the repoFullName keys
 * already held, then re-sorting. A repo's rank can genuinely change
 * after a Load More; that is intentional, since the ranking signal is
 * "how many matches in everything fetched so far".
 *
 * v0.44 (rate-limit event, Load More only): a Load More that comes back
 * 403 with X-RateLimit-Remaining: 0 is GitHub's Search API rate limit,
 * not a generic error. [events] reports [TopReposEvent.RateLimited]
 * once and canLoadMore/currentPage are deliberately left untouched, so
 * the button stays ready to tap again once the limit resets. Events are
 * a Channel, not part of the state, so each is delivered exactly once
 * rather than re-shown to every new collector.
 *
 * v0.45 (PERSISTENT CACHE): load() checks the cache FIRST for the
 * (language, mode) pair: a hit populates the state instantly with zero
 * network calls, including currentPage and canLoadMore, so Load More
 * resumes where a previous session left off. A miss falls through to the
 * network path. Every SUCCESSFUL fetch (initial load and every Load
 * More) saves the full accumulated repoGroups plus currentPage and
 * canLoadMore back to the cache. Fresh data for an already-cached pair
 * is never automatic; see checkForNewRepos().
 *
 * v0.49 ("Cache-Preserving Refresh"): what used to be refresh() forced a
 * network fetch through load(), whose non-Load-More branch groups into
 * an EMPTY starting list, replacing the entire accumulated list with
 * just page 1: with 200 repos built from ten Load More taps, one refresh
 * discarded nine taps' worth, a real data-loss bug. checkForNewRepos()
 * replaces it: the same one-call page-1 fetch, applied through
 * updateIssuesInRepoGroups(), never replacing.
 *
 * Why this is a singleton: the list state must live somewhere that
 * outlives any one composition. open() resets it for a fresh visit to
 * the screen, so nothing from an earlier visit leaks into the next, and
 * cancels any work still running from that visit. The work runs in the
 * shared [AppScope], not in a ViewModel's.
 *
 * The Koin module builds the one app-wide instance; tests build their
 * own over fakes.
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
        // When the token changes (saved in Settings) while the list is
        // stuck on a token problem — none set, or one GitHub rejected —
        // clear that problem and try again with the new token. drop(1)
        // skips the value the flow already holds when collection starts.
        // A token change while the list is working is ignored: the next
        // request just uses the new token.
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
    }

    /**
     * Starts a fresh visit to the screen: cancels work left over from the
     * previous visit, clears the state and any undelivered events, and
     * loads (cache first). Equivalent to what constructing the ViewModel
     * used to do.
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

    /**
     * Switches the Issue Mode in place and reloads, the same shape
     * selectLanguage() has. A no-op if the tapped mode is already active,
     * avoiding a pointless reload.
     */
    fun selectMode(mode: IssueLabelFilter) {
        if (!shouldSwitchMode(modeRepository.mode.value, mode)) return
        modeRepository.setMode(mode)
        load(_state.value.selectedLanguage)
    }

    /** Retries the INITIAL load — for when the list is empty because the first fetch failed. Unrelated to retryLoadMore(), which retries a failed Load More instead. */
    fun retry() = load(_state.value.selectedLanguage)

    /**
     * Fetches the next page and adds it to what's already held (see the
     * class comment for why "add to the existing cards," not "append"). A
     * no-op if a Load More is already in flight or canLoadMore is already
     * false, so it is safe to wire straight to a button.
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
     * v0.49 ("Cache-Preserving Refresh"): answers "what's new since I last
     * looked?": fetches page 1 exactly as a fresh load would, but UPDATES
     * repoGroups with the result instead of replacing them — new repos
     * are added, existing repos gain any new matches, and an
     * already-present issue's fields (comment count, labels, updated
     * time) are refreshed to the fetched copy via
     * updateIssuesInRepoGroups() (data/RepoIssueGrouping.kt), the
     * replace-by-id variant of addIssuesToRepoGroups(), whose own
     * skip-duplicate behavior stays exactly as it is for Load More.
     *
     * currentPage/canLoadMore are deliberately left untouched (unlike
     * fetchPage(), which always sets currentPage = page just fetched) —
     * this only ever fetches page 1 again, and a page-1 fetch must never
     * reset how far Load More has already gotten, or the data this exists
     * to stop discarding would just be lost a different way.
     */
    fun checkForNewRepos() {
        if (isCheckingForNewRepos || !hasTokenNow()) return
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

                        // Captured out of the update{} lambda (which may retry
                        // under contention, same pattern fetchPage() uses below)
                        // rather than snapshotting currentPage/canLoadMore
                        // before this suspending call — a concurrent Load More
                        // completing while this fetch was in flight would
                        // otherwise have its newer currentPage overwritten by a
                        // stale value when this writes back to the cache.
                        //
                        // This repoGroups is the NEW list this fetch produces; current.repoGroups below is
                        // the list held before it. Same type, different data: don't conflate them.
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
                    IssueSearchResult.NoToken -> Unit
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

    /**
     * v0.49: clears the ENTIRE persistent cache — see
     * RepoIssueGroupRepository.clearAll()'s doc comment for why this isn't
     * scoped to just the active (language, mode) pair yet — THEN
     * reloads immediately, rather than leaving already-visible data held
     * that the cache backing it no longer has. load() naturally falls
     * through to a fresh network fetch here since the cache it would have
     * checked was just cleared.
     */
    fun clearCache() {
        launchInVisit {
            cacheRepository.clearAll()
            load(_state.value.selectedLanguage)
        }
    }

    /**
     * v0.50 ("Refresh this repo" write-back): re-reads the cache for the
     * CURRENT (language, mode) pair and adopts whatever it finds — called
     * by TopReposScreen every time it's navigated back to, so a repo
     * refreshed from RepoIssuesScreen shows its updated match count (or
     * disappears entirely, if it had zero open issues left) here too,
     * without a second network call.
     *
     * RepoIssuesStateHolder and this state holder have no direct handle to each
     * other, so RepoIssuesStateHolder writes straight to
     * RepoIssueGroupRepository, the same persistence layer this state holder
     * reads from and writes to, and this function pulls that change back
     * into the held state.
     *
     * A no-op if there's nothing cached for this combination (it was
     * cleared, or was never loaded from Top Repos) — the state is left
     * exactly as it is rather than blanked out.
     */
    fun syncFromCache() {
        if (!hasTokenNow()) return
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
        if (!hasTokenNow()) {
            _state.update { it.copy(noTokenConfigured = true, isLoading = false) }
            return
        }

        // Defensive fallback only — DiscoverNavHost never creates this
        // screen while the mode is null. See IssueModeRepository.
        val activeMode = resolveActiveMode(modeRepository.mode.value)

        // Fresh load (language/mode switch, retry, or a reload right
        // after Clear Cache — see that function): reset pagination-error
        // state, but deliberately leave repoGroups AS-IS until new data
        // actually arrives — clearing it here would blank the screen
        // behind a spinner on every language switch, which isn't how
        // this screen has ever behaved. The old list gets replaced
        // outright once either the cache-hit path below or fetchPage's
        // non-Load-More path succeeds.
        _state.update {
            it.copy(
                currentPage = 0,
                canLoadMore = true,
                loadMoreFailure = null,
            )
        }

        launchInVisit {
            _state.update { it.copy(isLoading = true, loadFailure = null, noTokenConfigured = false) }
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

    /**
     * Fills in the rate-limit chip when a list loaded from the cache and
     * no search response has supplied the numbers yet, so the chip shows
     * real values with the first list instead of "—" until some later
     * live search happens. The lookup itself (GitHub's free /rate_limit
     * endpoint, and ignoring a failure) lives in IssueSearchRepository.
     */
    private fun refreshRateLimitIfUnknown() {
        launchInVisit { searchRepository.loadRateLimitIfUnknown() }
    }

    /** Whether a token is in use right now. */
    private fun hasTokenNow(): Boolean = tokenRepository.effectiveToken.value.isNotBlank()

    private fun fetchPage(language: LanguageFilter, page: Int, isLoadMore: Boolean) {
        if (!hasTokenNow()) {
            _state.update { it.copy(noTokenConfigured = true, isLoading = false) }
            return
        }

        // Defensive fallback only — see load().
        val activeMode = resolveActiveMode(modeRepository.mode.value)

        launchInVisit {
            if (isLoadMore) {
                _state.update { it.copy(isLoadingMore = true, loadMoreFailure = null) }
            } else {
                _state.update { it.copy(isLoading = true, loadFailure = null, noTokenConfigured = false) }
            }
            try {
                when (val result = searchRepository.searchTopRepos(activeMode, language, page)) {
                    is IssueSearchResult.Success -> {
                        val newIssues = result.page.issues
                        val totalCount = result.page.totalCount

                        // Computed inside the update{} lambda (which may retry
                        // under contention) and captured out to these vars so
                        // the SAME values that were actually committed to
                        // the state are what get persisted to the cache below —
                        // not a second, potentially different, independently
                        // recomputed pair of values.
                        //
                        // This repoGroups is the NEW list this page produces; state.repoGroups below is
                        // the list held before it. Same type, different data: don't conflate them.
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
                            // Deliberately leave canLoadMore/currentPage untouched —
                            // see class comment. Only the footer's loading
                            // state clears; the button itself is unaffected.
                            _state.update { it.copy(isLoadingMore = false) }
                            eventChannel.trySend(TopReposEvent.RateLimited)
                            return@launchInVisit
                        }
                        // 401 specifically means GitHub rejected the configured
                        // credentials (expired, revoked, or simply wrong) — GitHub's
                        // own distinction from a 403 rate-limit response, handled
                        // separately above. Surfaced via tokenRejected rather than
                        // just a generic failure so the rate-limit chip's tap
                        // handler can tell "no token" and "a token that doesn't
                        // work" apart.
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
                    IssueSearchResult.NoToken ->
                        _state.update { it.copy(noTokenConfigured = true, isLoading = false, isLoadingMore = false) }
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
