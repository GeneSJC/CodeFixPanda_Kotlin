package com.example.githubissuefinder.ui.discover

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.githubissuefinder.data.IssueModeRepository
import com.example.githubissuefinder.data.RateLimitState
import com.example.githubissuefinder.data.RepoIssueGroupCache
import com.example.githubissuefinder.data.formatRelativeTime
import com.example.githubissuefinder.data.mergeReplacingByIssueId
import com.example.githubissuefinder.data.parseRateLimitHeaders
import com.example.githubissuefinder.data.resolveActiveMode
import com.example.githubissuefinder.data.shouldSwitchMode
import com.example.githubissuefinder.data.toIssue
import com.example.githubissuefinder.model.Issue
import com.example.githubissuefinder.model.IssueLabelFilter
import com.example.githubissuefinder.model.LanguageFilter
import com.example.githubissuefinder.model.RepoIssueGroup
import com.example.githubissuefinder.network.GitHubApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PER_PAGE = 30

/**
 * GitHub's Search API hard cap — it will not return results past this
 * point for any query, however many actually match (this is a
 * documented GitHub limitation, not something this app imposes).
 * canRequestNextPage() uses this to stop offering Load More before a
 * request would fail, rather than surfacing that as an error.
 */
private const val SEARCH_API_RESULT_CAP = 1000

/**
 * v0.42 — THE DATA-LAYER SWAP. This is a full rewrite of how this
 * screen builds its list, replacing everything CM 1 through v0.41
 * built the pipeline around. Read this before touching this file.
 *
 * OLD PIPELINE (v0.25–v0.41): repository search first (ranked via
 * scoreAndRankRepos — stars/forks/recency/velocity), take the top 20,
 * THEN annotate each one with a separate per-repo issue-count check
 * for whichever Issue Mode is active. Cost: ~21 Search API calls per
 * load. A repo could rank highly on general health while having ZERO
 * matching issues for the active mode — the known, explicitly-flagged
 * limitation this whole rewrite exists to eliminate.
 *
 * NEW PIPELINE (this CM): query GitHub's Issue Search API DIRECTLY for
 * the active mode's label (the exact same query shape FeedViewModel
 * has always used), then GROUP the returned issues by their parent
 * repo (repository_url, via Issue.repoFullName). Each group becomes
 * one RepoIssueGroup — see that model's doc comment. Cost: ONE Search
 * API call per load, full stop. And because a repo only appears here
 * at all if it had a matching issue in this batch, a repo with zero
 * matching issues literally cannot be shown — not graying, not a
 * dead-end tap, structurally impossible.
 *
 * WHAT THIS COSTS: RepoIssueGroup carries none of Repo's old
 * stars/forks/description/composite-score fields — GitHub's issue
 * search response never included them, only ever came from the
 * now-removed per-repo annotation step and the repo search itself.
 * Ranking here is match-count only (RepoIssueGroup.matchCount,
 * descending) — genuinely less rich than the old composite score, and
 * a deliberate, discussed tradeoff, not an oversight: per-repo
 * enrichment (a bounded number of extra calls, one per UNIQUE repo in
 * the result page rather than one per issue) remains a real, optional
 * follow-up if match-count-only ranking ever feels too thin once seen
 * running — not attempted here.
 *
 * THE COOLDOWN IS GONE. v0.35's 60-second cooldown existed specifically
 * because the old pipeline's ~21-calls-per-load made a second load
 * within 60 seconds mathematically risk the 30/min Search API cap.
 * This pipeline's ENTIRE load is one call — the arithmetic that
 * justified the cooldown no longer applies, so onLanguageSelected()
 * below is a plain, unblocked, Unit-returning function now, and
 * TopReposScreen no longer needs the accepted/blocked Snackbar dance
 * either. A real, direct side benefit of this rewrite, not a
 * separately-decided simplification.
 *
 * No sample-data fallback yet (see TopReposUiState) — this screen
 * still requires a configured GitHub token, unchanged from before.
 *
 * The Issue Mode is read from IssueModeRepository (see its doc comment
 * for why that is the one owner of it), via a constructor argument so
 * a test can supply a fake. [mode] re-exposes it so TopReposScreen
 * observes it through this ViewModel rather than reading a global.
 * onModeSelected(mode) below is a plain in-place switch — asks the
 * repository to change the mode and reloads, the same
 * shape onLanguageSelected() already has — reached from two explicit
 * items at the bottom of TopReposScreen's hamburger menu ("Find Good
 * First Issues" / "Find Help Wanted Issues"), NOT a resurrection of
 * the old v0.41 title-bar dropdown that was removed earlier in this
 * CM sequence. See TopReposScreen's doc comment for the full
 * reasoning, including why this is a direct in-place switch rather
 * than navigating back to IssueModeGateScreen.
 *
 * v0.43 (PAGINATION + CROSS-PAGE DEDUP): load() previously fetched
 * exactly one page and stopped — whatever groups formed from GitHub's
 * default 30 results were the whole list, with no way to see more.
 * loadMore() adds a second, explicit page fetch, and — this is the
 * part worth reading carefully — the result is MERGED into the
 * existing repoGroups, not appended as new cards. A repo's matching
 * issues are not guaranteed to land on a single page: if "Apps
 * Android Commons" has 5 matching issues and 3 landed on page 1, the
 * other 2 can easily land on page 2. Naively appending page 2's
 * grouped results would show "Apps Android Commons" as a SECOND,
 * separate card with match count 2, instead of updating the existing
 * card to match count 5. mergeIntoGroups() below is what prevents
 * that — every Load More re-groups its new issues against the
 * repoFullName keys already on screen before anything is added or
 * re-sorted.
 *
 * A repo's rank can genuinely change after a Load More (a repo that
 * picks up more matches on this page can overtake one that hasn't) —
 * that's intentional, not a bug to suppress: the ranking signal is
 * "how many matches in everything fetched so far," so it should update
 * as more is fetched, the same way it would if all of it had come back
 * in one page to begin with.
 *
 * v0.44 (rate-limit Snackbar, Load More only): if a Load More fetch
 * comes back 403 with X-RateLimit-Remaining: 0, that's specifically
 * GitHub's Search API rate limit, not a generic error — snackbarEvents
 * emits a one-shot message for TopReposScreen to show, and
 * deliberately does NOT touch canLoadMore or currentPage: the button
 * stays exactly as it was, ready to tap again once the limit resets,
 * rather than landing in the same inline-error-with-Retry state a
 * genuine failure would. Scoped to Load More specifically, not the
 * initial load. snackbarEvents is a Channel, not part of
 * TopReposUiState, so each event is delivered exactly once rather than
 * re-shown on every new collector (e.g. after a configuration change).
 *
 * v0.45 (PERSISTENT CACHE): now an AndroidViewModel instead of a plain
 * ViewModel — the only reason is Context access for
 * RepoIssueGroupCache's file I/O (Compose's viewModel() constructs
 * AndroidViewModel subclasses automatically via the default factory
 * when hosted from an Activity, so no extra wiring is needed at the
 * call site in TopReposScreen). load() now checks the cache FIRST for
 * the (language, mode) pair being requested: a hit populates uiState
 * instantly with zero network calls — including currentPage and
 * canLoadMore, so Load More resumes exactly where a previous session
 * left off, not reset back to page 1. A miss falls through to the
 * exact v0.42/v0.43 network path unchanged. Every SUCCESSFUL fetch
 * (both the initial load AND every Load More) saves its result back to
 * the cache — Load More progress persists too, not just the first
 * page, since RepoIssueGroupCache.CacheEntry stores the full
 * accumulated repoGroups plus currentPage/canLoadMore, not a single
 * page (see that class's own doc comment for why this had to wait
 * until pagination existed to be designed correctly). Getting fresh
 * data for an already-cached combination is never automatic — see
 * checkForNewRepos() and RepoIssueGroupCache's own doc comment for why.
 *
 * The cache check itself runs inside its own viewModelScope.launch
 * inside load(); on a cache MISS it calls fetchPage(), which launches
 * its own separate coroutine internally rather than being restructured
 * to share the outer one — a small, deliberate inefficiency (one extra
 * coroutine hop) traded for not having to touch fetchPage's own
 * structure, which is unchanged from v0.43/v0.44 otherwise.
 *
 * v0.49 ("Cache-Preserving Refresh"): what used to be refresh() forced
 * a network fetch through load(), which — because fetchPage()'s
 * non-Load-More branch calls groupByRepo(), i.e. mergeIntoGroups()
 * against an EMPTY starting list — replaced the entire accumulated
 * repoGroups with just page 1's issues. With 200 repos built from ten
 * Load More taps, one refresh discarded the results of nine of them: a
 * real data-loss bug, not a labelling problem, since it defeated the
 * entire point of the v0.45 cache. checkForNewRepos() replaces it: same
 * one-call page-1 fetch, but merged in via the new
 * mergeReplacingByIssueId() (see that function's own doc comment),
 * never replacing. See this CM's own plan doc
 * (GitHubIssueFinder_CachePreservingRefresh_Plan_2026-09-25.pdf) for
 * the full reasoning and the discussion that surfaced this.
 */
class TopReposViewModel @JvmOverloads constructor(
    application: Application,
    private val modeRepository: IssueModeRepository = IssueModeRepository.instance,
) : AndroidViewModel(application) {

    /**
     * The active Issue Mode, or null while none is chosen (which
     * DiscoverNavHost prevents from ever being observed here — see
     * IssueModeGateScreen). @JvmOverloads on the constructor above keeps
     * the single-Application-argument constructor that Compose's default
     * AndroidViewModel factory looks for.
     */
    val mode: StateFlow<IssueLabelFilter?> = modeRepository.mode

    private val _uiState = MutableStateFlow(TopReposUiState())
    val uiState: StateFlow<TopReposUiState> = _uiState.asStateFlow()

    private val _snackbarEvents = Channel<String>(Channel.BUFFERED)
    val snackbarEvents: Flow<String> = _snackbarEvents.receiveAsFlow()

    /** Guards checkForNewRepos() against overlapping calls — not exposed via TopReposUiState since nothing in the UI needs to react to it (the menu item is fire-and-forget; feedback arrives via snackbarEvents once the call resolves). */
    private var isCheckingForNewRepos = false

    init {
        load(uiState.value.selectedLanguage)
    }

    fun onLanguageSelected(language: LanguageFilter) {
        _uiState.update { it.copy(selectedLanguage = language) }
        load(language)
    }

    /**
     * Handler for the two hamburger-menu items TopReposScreen offers
     * ("Find Good First Issues" / "Find Help Wanted Issues") — see
     * class doc comment for why this exists as a direct, in-place
     * switch rather than navigating anywhere. A no-op if the tapped
     * mode is already active, same guard the old (removed) dropdown
     * had, avoiding a pointless reload.
     */
    fun onModeSelected(mode: IssueLabelFilter) {
        if (!shouldSwitchMode(modeRepository.mode.value, mode)) return
        modeRepository.setMode(mode)
        load(uiState.value.selectedLanguage)
    }

    /** Retries the INITIAL load — for when the list is empty because the first fetch failed. Unrelated to retryLoadMore() below, which retries a failed Load More instead. */
    fun retry() = load(uiState.value.selectedLanguage)

    /**
     * Fetches the next page and merges it into what's already shown —
     * see class doc comment for why "merge," not "append." A no-op if
     * a Load More is already in flight or canLoadMore is already
     * false, so this is safe to wire directly to a button's onClick
     * without the caller needing to check state first.
     */
    fun loadMore() {
        val state = uiState.value
        if (state.isLoadingMore || !state.canLoadMore) return
        fetchPage(
            language = state.selectedLanguage,
            page = state.currentPage + 1,
            isLoadMore = true,
        )
    }

    /** Retries a Load More that failed — distinct from retry(), which is for the initial load. Clears loadMoreError and re-attempts the SAME next page, not a fresh load. */
    fun retryLoadMore() {
        _uiState.update { it.copy(loadMoreError = null) }
        loadMore()
    }

    /**
     * v0.49 ("Cache-Preserving Refresh"): replaces what "Refresh" used
     * to do (bypass the cache, REPLACE the whole list with just page
     * 1's issues — silently discarding everything Load More had
     * accumulated, a real data-loss bug, not a labelling problem — see
     * this CM's own plan doc). Answers "what's new since I last
     * looked?": fetches page 1 exactly as a fresh load would, but MERGES
     * the result into repoGroups instead of replacing it — new repos
     * are added, existing repos gain any new matches, and an
     * already-present issue's fields (comment count, labels, updated
     * time) get refreshed to the fetched copy via
     * data.RepoIssueGroupMerge.kt's mergeReplacingByIssueId() — a
     * dedicated replace-by-id variant of mergeIntoGroups() below, kept
     * as a standalone top-level function specifically so it's directly
     * unit-testable (see its own doc comment for why) — that function's
     * own skip-duplicate behavior stays exactly as it is for Load More,
     * which is correct there and would be wrong here.
     *
     * currentPage/canLoadMore are deliberately left untouched (unlike
     * fetchPage(), which always sets currentPage = page just fetched) —
     * this only ever fetches page 1 again, and a page-1 fetch must never
     * reset how far Load More has already gotten, or the very data this
     * CM exists to stop discarding would just be lost a different way.
     */
    fun checkForNewRepos() {
        if (isCheckingForNewRepos || !GitHubApiClient.hasToken) return
        val activeMode = resolveActiveMode(modeRepository.mode.value)
        val state = uiState.value
        val language = state.selectedLanguage

        viewModelScope.launch {
            isCheckingForNewRepos = true
            try {
                val query = buildString {
                    append("label:\"${activeMode.queryValue}\"")
                    append(" state:open")
                    language.queryValue?.let { append(" language:$it") }
                }
                val response = withContext(Dispatchers.IO) {
                    GitHubApiClient.api.searchIssues(query = query, perPage = PER_PAGE, page = 1)
                }
                val body = response.body()
                if (!response.isSuccessful || body == null) {
                    _snackbarEvents.trySend(
                        "Couldn't check for new repos (HTTP ${response.code()})",
                    )
                    return@launch
                }
                RateLimitState.update(parseRateLimitHeaders(response.headers()))
                val fetchedIssues = body.items.map { it.toIssue(languageFilter = language.queryValue) }

                // Captured out of the update{} lambda (which may retry
                // under contention, same pattern fetchPage() uses below)
                // rather than snapshotting state.currentPage/canLoadMore
                // before this suspending call — a concurrent Load More
                // completing while this fetch was in flight would
                // otherwise have its newer currentPage overwritten by a
                // stale value when this writes back to the cache.
                var mergedGroups: List<RepoIssueGroup> = emptyList()
                var newRepoCount = 0
                var newIssueCount = 0
                var currentPageAtCommit = state.currentPage
                var canLoadMoreAtCommit = state.canLoadMore
                var previousUpdatedAtMs = state.lastUpdatedAtMs
                val fetchedAtMs = System.currentTimeMillis()
                _uiState.update { current ->
                    val result = mergeReplacingByIssueId(current.repoGroups, fetchedIssues)
                    mergedGroups = result.groups
                    newRepoCount = result.newRepoCount
                    newIssueCount = result.newIssueCount
                    currentPageAtCommit = current.currentPage
                    canLoadMoreAtCommit = current.canLoadMore
                    previousUpdatedAtMs = current.lastUpdatedAtMs
                    current.copy(
                        repoGroups = mergedGroups,
                        totalIssueCount = body.totalCount,
                        lastUpdatedAtMs = fetchedAtMs,
                    )
                }
                withContext(Dispatchers.IO) {
                    RepoIssueGroupCache.save(
                        context = getApplication(),
                        language = language,
                        mode = activeMode,
                        repoGroups = mergedGroups,
                        totalIssueCount = body.totalCount,
                        currentPage = currentPageAtCommit,
                        canLoadMore = canLoadMoreAtCommit,
                    )
                }
                _snackbarEvents.trySend(
                    if (newRepoCount == 0 && newIssueCount == 0) {
                        "Nothing new since ${formatRelativeTime(previousUpdatedAtMs ?: fetchedAtMs)}"
                    } else {
                        "$newRepoCount new repo${if (newRepoCount == 1) "" else "s"}, " +
                            "$newIssueCount new issue${if (newIssueCount == 1) "" else "s"}"
                    },
                )
            } catch (e: Exception) {
                _snackbarEvents.trySend("Couldn't check for new repos: ${e.message ?: "unknown error"}")
            } finally {
                isCheckingForNewRepos = false
            }
        }
    }

    /**
     * v0.49: clears the ENTIRE persistent cache — see
     * RepoIssueGroupCache.clearAll()'s doc comment for why this isn't
     * scoped to just the active (language, mode) pair yet — THEN
     * reloads the current screen immediately, rather than leaving
     * already-visible data on screen that the cache backing it no
     * longer has (the previous behavior: the list stayed put until the
     * user switched chips or reopened the screen). load() naturally
     * falls through to a fresh network fetch here since the cache it
     * would have checked was just cleared.
     */
    fun clearCache() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                RepoIssueGroupCache.clearAll(getApplication())
            }
            load(uiState.value.selectedLanguage)
        }
    }

    /**
     * v0.50 ("Refresh this repo" write-back): re-reads the cache for the
     * CURRENT (language, mode) pair and adopts whatever it finds —
     * called by TopReposScreen every time it's navigated back to (see
     * that screen's own comment on where), so a repo refreshed from
     * RepoIssuesScreen shows its updated match count (or disappears
     * entirely, if it had zero open issues left) here too, without
     * needing a second network call.
     *
     * This is the resolution to the write-back design question the
     * v0.49 plan doc left open ("shared nav-graph scope vs. re-reading
     * the cache on return"): RepoIssuesScreen and this screen have no
     * direct handle to each other's ViewModel instances (each gets its
     * own via viewModel()), so RepoIssuesViewModel writes straight to
     * RepoIssueGroupCache — the same persistence layer this ViewModel
     * already reads from and writes to — and this function is what
     * pulls that change back into THIS screen's in-memory state, rather
     * than the two ViewModels talking to each other directly.
     *
     * A no-op if there's nothing cached for this combination (e.g. it
     * was cleared, or this combination was never actually loaded from
     * Top Repos to begin with) — repoGroups etc. are left exactly as
     * they are rather than being blanked out.
     */
    fun syncFromCache() {
        if (!GitHubApiClient.hasToken) return
        val activeMode = resolveActiveMode(modeRepository.mode.value)
        val language = uiState.value.selectedLanguage
        viewModelScope.launch {
            val cached = withContext(Dispatchers.IO) {
                RepoIssueGroupCache.load(getApplication(), language, activeMode)
            } ?: return@launch
            _uiState.update {
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
        if (!GitHubApiClient.hasToken) {
            _uiState.update { it.copy(noTokenConfigured = true, isLoading = false) }
            return
        }

        // Defensive fallback only — DiscoverNavHost never creates this
        // screen (and therefore this ViewModel) while the mode is null.
        // See IssueModeRepository.
        val activeMode = resolveActiveMode(modeRepository.mode.value)

        // Fresh load (language/mode switch, retry, or a reload right
        // after Clear Cache — see that function): reset pagination-error
        // state, but deliberately leave repoGroups AS-IS until new data
        // actually arrives — clearing it here would blank the screen
        // behind a spinner on every language switch, which isn't how
        // this screen has ever behaved. The old list gets replaced
        // outright once either the cache-hit path below or fetchPage's
        // non-merge path succeeds.
        _uiState.update {
            it.copy(
                currentPage = 0,
                canLoadMore = true,
                loadMoreError = null,
            )
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, noTokenConfigured = false) }
            val cached = withContext(Dispatchers.IO) {
                RepoIssueGroupCache.load(getApplication(), language, activeMode)
            }
            if (cached != null) {
                _uiState.update {
                    it.copy(
                        repoGroups = cached.repoGroups,
                        totalIssueCount = cached.totalIssueCount,
                        currentPage = cached.currentPage,
                        canLoadMore = cached.canLoadMore,
                        lastUpdatedAtMs = cached.fetchedAtMs,
                        isLoading = false,
                    )
                }
            } else {
                fetchPage(language = language, page = 1, isLoadMore = false)
            }
        }
    }

    private fun fetchPage(language: LanguageFilter, page: Int, isLoadMore: Boolean) {
        if (!GitHubApiClient.hasToken) {
            _uiState.update { it.copy(noTokenConfigured = true, isLoading = false) }
            return
        }

        // Defensive fallback only — DiscoverNavHost never creates this
        // screen (and therefore this ViewModel) while the mode is null.
        // See IssueModeRepository.
        val activeMode = resolveActiveMode(modeRepository.mode.value)

        viewModelScope.launch {
            if (isLoadMore) {
                _uiState.update { it.copy(isLoadingMore = true, loadMoreError = null) }
            } else {
                _uiState.update { it.copy(isLoading = true, errorMessage = null, noTokenConfigured = false) }
            }
            try {
                val query = buildString {
                    append("label:\"${activeMode.queryValue}\"")
                    append(" state:open")
                    language.queryValue?.let { append(" language:$it") }
                }
                val response = withContext(Dispatchers.IO) {
                    GitHubApiClient.api.searchIssues(query = query, perPage = PER_PAGE, page = page)
                }
                val body = response.body()
                if (!response.isSuccessful || body == null) {
                    val rateLimitInfo = parseRateLimitHeaders(response.headers())
                    val isRateLimited = response.code() == 403 && rateLimitInfo?.remaining == 0
                    if (isLoadMore && isRateLimited) {
                        // Deliberately leave canLoadMore/currentPage untouched —
                        // see class doc comment. Only the footer's loading
                        // state clears; the button itself is unaffected.
                        _uiState.update { it.copy(isLoadingMore = false) }
                        _snackbarEvents.trySend(
                            "GitHub's search rate limit was reached — try again shortly",
                        )
                        return@launch
                    }
                    // 401 specifically means GitHub rejected the configured
                    // credentials (expired, revoked, or simply wrong) — GitHub's
                    // own distinction from a 403 rate-limit response, handled
                    // separately above. Surfaced via tokenRejected rather than
                    // just errorMessage's generic text so the rate-limit chip's
                    // tap handler can tell "no token" and "a token that doesn't
                    // work" apart. See TopReposScreen's TokenProblemDialog.
                    val isTokenRejected = response.code() == 401
                    val message = "GitHub returned an error (HTTP ${response.code()})"
                    _uiState.update {
                        if (isLoadMore) {
                            it.copy(isLoadingMore = false, loadMoreError = message)
                        } else {
                            it.copy(isLoading = false, errorMessage = message, tokenRejected = isTokenRejected)
                        }
                    }
                    return@launch
                }
                RateLimitState.update(parseRateLimitHeaders(response.headers()))
                val newIssues = body.items.map { it.toIssue(languageFilter = language.queryValue) }

                // Computed inside the update{} lambda (which may retry
                // under contention) and captured out to these vars so
                // the SAME values that were actually committed to
                // uiState are what get persisted to the cache below —
                // not a second, potentially different, independently
                // recomputed pair of values.
                var mergedGroups: List<RepoIssueGroup> = emptyList()
                var newCanLoadMore = false
                val fetchedAtMs = System.currentTimeMillis()
                _uiState.update { state ->
                    mergedGroups = if (isLoadMore) {
                        mergeIntoGroups(state.repoGroups, newIssues)
                    } else {
                        groupByRepo(newIssues)
                    }
                    newCanLoadMore = canRequestNextPage(fetchedThisPage = newIssues.size, justFetchedPage = page)
                    state.copy(
                        repoGroups = mergedGroups,
                        isLoading = false,
                        isLoadingMore = false,
                        totalIssueCount = body.totalCount,
                        currentPage = page,
                        canLoadMore = newCanLoadMore,
                        lastUpdatedAtMs = fetchedAtMs,
                        tokenRejected = false,
                    )
                }
                withContext(Dispatchers.IO) {
                    RepoIssueGroupCache.save(
                        context = getApplication(),
                        language = language,
                        mode = activeMode,
                        repoGroups = mergedGroups,
                        totalIssueCount = body.totalCount,
                        currentPage = page,
                        canLoadMore = newCanLoadMore,
                    )
                }
            } catch (e: Exception) {
                val message = "Couldn't load issues: ${e.message ?: "unknown error"}"
                _uiState.update {
                    if (isLoadMore) {
                        it.copy(isLoadingMore = false, loadMoreError = message)
                    } else {
                        it.copy(isLoading = false, errorMessage = message)
                    }
                }
            }
        }
    }

    /**
     * False once there's nothing left worth requesting: either this
     * page came back short (fewer than PER_PAGE items — the true end
     * of this query's results, not just an empty page), or the NEXT
     * page would start past GitHub's 1,000-result Search API cap.
     * Checked against the page just fetched, not the total the API
     * reports — total_count itself is only an estimate once a query
     * has more matches than the cap allows, so it isn't trustworthy
     * for this decision on its own.
     */
    private fun canRequestNextPage(fetchedThisPage: Int, justFetchedPage: Int): Boolean {
        if (fetchedThisPage < PER_PAGE) return false
        val nextPageStartIndex = justFetchedPage * PER_PAGE
        return nextPageStartIndex < SEARCH_API_RESULT_CAP
    }

    /**
     * Groups already-fetched issues by parent repo and sorts the
     * resulting groups by match count, descending — the v0.42 ranking
     * signal (see class doc comment for why this replaces the old
     * composite score). No artificial cap on the number of groups
     * shown — whatever groups form from the page(s) fetched so far are
     * all shown, which bounds card count naturally without a separate
     * constant to keep in sync.
     */
    private fun groupByRepo(issues: List<Issue>): List<RepoIssueGroup> =
        mergeIntoGroups(emptyList(), issues)

    /**
     * v0.43: the actual cross-page merge — see class doc comment for
     * the motivating scenario (one repo's matches spread across two
     * pages). Re-keys existing groups by repoFullName in a
     * LinkedHashMap (preserving their current order, so ties in the
     * final sort stay deterministic rather than depending on an
     * unordered HashMap's iteration), folds each new issue into its
     * repo's existing group if one exists (skipping it if that exact
     * issue id is somehow already present — defensive, in case
     * GitHub's result set shifts slightly between two page requests),
     * or starts a new group if this is the first issue seen for that
     * repo, then re-sorts everything by the now-current match count.
     * groupByRepo() above is just this function called with an empty
     * starting list — the very first page is "merged" into nothing.
     */
    private fun mergeIntoGroups(existing: List<RepoIssueGroup>, newIssues: List<Issue>): List<RepoIssueGroup> {
        val byRepo = LinkedHashMap<String, RepoIssueGroup>()
        existing.forEach { byRepo[it.repoFullName] = it }
        newIssues.forEach { issue ->
            val current = byRepo[issue.repoFullName]
            if (current == null) {
                byRepo[issue.repoFullName] = RepoIssueGroup(
                    repoOwner = issue.repoOwner,
                    repoName = issue.repoName,
                    repoFullName = issue.repoFullName,
                    repoDisplayTitle = issue.repoDisplayTitle,
                    issues = listOf(issue),
                )
            } else if (current.issues.none { it.id == issue.id }) {
                byRepo[issue.repoFullName] = current.copy(issues = current.issues + issue)
            }
        }
        return byRepo.values.sortedByDescending { it.matchCount }
    }
}
