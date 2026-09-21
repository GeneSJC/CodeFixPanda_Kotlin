package com.example.githubissuefinder.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.githubissuefinder.data.RateLimitState
import com.example.githubissuefinder.data.parseRateLimitHeaders
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
 * IssueMode.current is read directly in load(), same "shared object,
 * not threaded through the constructor" pattern established in v0.41
 * — see IssueMode.kt. onModeSelected(mode) below is a plain in-place
 * switch — updates IssueMode.current directly and reloads, the same
 * shape onLanguageSelected() already has — reached from two explicit
 * items at the bottom of TopReposScreen's hamburger menu ("Find Good
 * First Issues" / "Find Help Wanted Issues"), NOT a resurrection of
 * the old v0.41 title-bar dropdown that was removed earlier in this
 * CM sequence. That dropdown sat directly in the title, visible and
 * tappable without any deliberate action — exactly what made it feel
 * accidental rather than a real choice, and why it was removed. Living
 * at the bottom of a menu the user has to deliberately open keeps the
 * "this is a real, considered choice" framing intact. See
 * TopReposScreen's doc comment for the full reasoning, including why
 * this is a direct in-place switch rather than navigating back to
 * IssueModeGateScreen (a gate-based design was fully worked out first,
 * then set aside — reusing the gate is the more scalable choice if a
 * third mode is ever added, but wasn't worth the added navigation
 * complexity for exactly two).
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
 * genuine failure would. This is intentionally the full extent of
 * rate-limit handling for now — a persistent visible meter (matching
 * Feed's existing RateLimitChip) is deliberately out of scope here,
 * parked as lower priority; this Snackbar is enough to know it CAN
 * happen without committing to where/how a fuller indicator eventually
 * lives. Scoped to Load More specifically, not the initial load, per
 * how this was asked for — switching language/mode into a rate limit
 * still just surfaces the existing generic errorMessage state.
 *
 * snackbarEvents is a Channel, not part of TopReposUiState — a
 * StateFlow field would re-emit its last value on every new collector
 * (e.g. after a configuration change), showing the same Snackbar again
 * for an event that already happened. A Channel only ever delivers
 * each event once, to whichever collector is listening at the time —
 * the correct shape for a genuinely one-shot UI event like this.
 */
class TopReposViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TopReposUiState())
    val uiState: StateFlow<TopReposUiState> = _uiState.asStateFlow()

    private val _snackbarEvents = Channel<String>(Channel.BUFFERED)
    val snackbarEvents: Flow<String> = _snackbarEvents.receiveAsFlow()

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
        if (mode == IssueMode.current) return
        IssueMode.current = mode
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

    private fun load(language: LanguageFilter) {
        if (!GitHubApiClient.hasToken) {
            _uiState.update { it.copy(noTokenConfigured = true, isLoading = false) }
            return
        }
        // Fresh load (language/mode switch or retry): reset pagination
        // state, but deliberately leave repoGroups AS-IS until the new
        // data actually arrives — clearing it here would blank the
        // screen behind a spinner on every language switch, which
        // isn't how this screen behaved before pagination existed. The
        // old list gets replaced outright once fetchPage's non-merge
        // path (isLoadMore = false) succeeds.
        _uiState.update {
            it.copy(
                currentPage = 0,
                canLoadMore = true,
                loadMoreError = null,
            )
        }
        fetchPage(language = language, page = 1, isLoadMore = false)
    }

    private fun fetchPage(language: LanguageFilter, page: Int, isLoadMore: Boolean) {
        if (!GitHubApiClient.hasToken) {
            _uiState.update { it.copy(noTokenConfigured = true, isLoading = false) }
            return
        }

        // Defensive fallback only — DiscoverNavHost never creates this
        // screen (and therefore this ViewModel) while IssueMode.current
        // is null. See IssueMode.kt.
        val activeMode = IssueMode.current ?: IssueLabelFilter.GOOD_FIRST_ISSUE

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
                    val message = "GitHub returned an error (HTTP ${response.code()})"
                    _uiState.update {
                        if (isLoadMore) {
                            it.copy(isLoadingMore = false, loadMoreError = message)
                        } else {
                            it.copy(isLoading = false, errorMessage = message)
                        }
                    }
                    return@launch
                }
                RateLimitState.update(parseRateLimitHeaders(response.headers()))
                val newIssues = body.items.map { it.toIssue(languageFilter = language.queryValue) }
                _uiState.update { state ->
                    val mergedGroups = if (isLoadMore) {
                        mergeIntoGroups(state.repoGroups, newIssues)
                    } else {
                        groupByRepo(newIssues)
                    }
                    state.copy(
                        repoGroups = mergedGroups,
                        isLoading = false,
                        isLoadingMore = false,
                        totalIssueCount = body.totalCount,
                        currentPage = page,
                        canLoadMore = canRequestNextPage(fetchedThisPage = newIssues.size, justFetchedPage = page),
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
