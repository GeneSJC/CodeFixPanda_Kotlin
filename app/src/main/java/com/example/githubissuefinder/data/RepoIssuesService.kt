package com.example.githubissuefinder.data

import com.example.githubissuefinder.model.Issue
import com.example.githubissuefinder.model.IssueLabelFilter
import com.example.githubissuefinder.model.LanguageFilter
import com.example.githubissuefinder.model.RepoIssueGroup
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The repo-issues data state: what the screen's list and refresh footer
 * are built from, and nothing about how it is shown. RepoIssuesService is
 * the one place it lives and changes; RepoIssuesViewModel maps it to
 * RepoIssuesUiState (which adds the message text).
 */
data class RepoIssuesState(
    val issues: List<Issue> = emptyList(),
    /** True while the one-shot complete fetch is in flight. The issues already held stay visible; only the footer reflects it. */
    val isRefreshing: Boolean = false,
    /** True once at least one refresh has completed successfully this visit; then [issues] is every open issue GitHub has for the repo and mode, up to one call's page size. */
    val hasRefreshed: Boolean = false,
    /** When the last successful refresh completed. Null until [hasRefreshed] is first true. */
    val lastRefreshedAtMs: Long? = null,
    /** Set when a refresh fails; [issues] is left exactly as it was. */
    val refreshFailure: SearchFailure? = null,
)

/**
 * The service layer for the repo-scoped issue list: the one place the
 * state of the repo being viewed lives (its issues, whether a refresh is
 * running, the last result), and the owner of the logic that changes it.
 * RepoIssuesViewModel maps this state to what the screen shows and
 * forwards the user's actions here. (The long design history below was
 * the ViewModel's class comment; it is about this logic, so it moved with
 * it.)
 *
 * The repo-scoped issue list — CM "B" of the Discover redesign
 * (option C from the architecture discussion): the drill-in that
 * every Discover CM since v0.25 has flagged as deferred, since tapping
 * a repo was previously a no-op.
 *
 * v0.42, REVISED — the in-screen label picker (LabelFilterRow, which
 * let someone peek at the OTHER label within a repo, independent of
 * the app's active Issue Mode) is REMOVED. The original draft of this
 * CM had explicitly flagged that as "a real, open question, not
 * decided in this CM" — it's now been decided: Mode is a single,
 * deliberate, whole-app choice made once at the gate screen, and
 * nothing downstream offers a second way to change it, no matter how
 * many levels deep into the app. Same reasoning that removed the Top
 * Repos mode dropdown, applied here for consistency.
 *
 * The direct consequence: for a long time this screen made NO
 * network call, ever, for any reason. Every issue this screen could
 * possibly show was already sitting in repoGroup.issues — that's
 * exactly what a repo showing up on Top Repos at all already means
 * (see RepoIssueGroup). loadIfNeeded() is still a pure, synchronous
 * assignment, unchanged.
 *
 * v0.46 ("Check for More Issues") was the one, deliberate exception to
 * that "zero network calls" property. v0.50 ("Refresh this repo" write-
 * back) renames and reshapes it — read this before assuming the old
 * shape still holds. repoGroup.issues only ever contains whatever this
 * repo's matches happened to be swept up while Top Repos was building
 * its list (bounded by however far Load More had gotten at the time).
 * A repo with more matching issues than that is a real, confirmed
 * completeness gap — this screen could be showing a partial view
 * without any way to know it, and closed issues can only ever be
 * discovered this way too (GitHub's issue search only returns OPEN
 * issues, so it can never itself report "these are now closed").
 * refreshThisRepo() closes both gaps with ONE explicit, user-initiated
 * fetch: `repo:owner/name label:"..." state:open`, scoped to just this
 * repo and the active mode, asking for up to REPO_ISSUES_MAX_PER_PAGE (100 —
 * GitHub's own per-call max) results in a single call. On success, this
 * REPLACES `issues` outright with the complete result (not an incremental update —
 * there's nothing to dedup against, this call already returns
 * everything for this query) — correct here specifically because it IS
 * the complete live set for this one repo, unlike Top Repos' own
 * checkForNewRepos(), which updates a list from a partial page.
 *
 * UNLIKE v0.46: the button never disappears after use — hasRefreshed
 * drives a small result line ("All 7 open issues · checked 2m ago"),
 * not a one-time switch to a dead end. Re-tappable any number of times
 * this session.
 *
 * WRITE-BACK (new in v0.50): a successful refresh also updates the Top
 * Repos cache entry this repo's group came from
 * (RepoIssueGroupRepository.replaceRepoGroup()), so the match count on the
 * Top Repos card the user navigated from agrees on return — without
 * that, this screen's own state could show a repo as having 3 open
 * issues while Top Repos still showed 7 until the next full Top Repos
 * refresh. If the complete fetch comes back EMPTY, that repo is
 * removed from the cache entirely (replaceRepoGroup(updatedGroup =
 * null)) — a repo can't legitimately appear on Top Repos with zero
 * matching issues (see RepoIssueGroup's own doc comment), so a refresh
 * finding none left is exactly the moment for it to leave that list.
 * TopReposService.syncFromCache() is what actually pulls this updated
 * cache entry back into Top Repos' own in-memory state when the user
 * navigates back — see that function's doc comment for why a re-read
 * on return, rather than a shared ViewModel reference, was the chosen
 * mechanism (RepoIssuesScreen and TopReposScreen have no direct handle
 * to each other's ViewModel instances).
 *
 * language (the LanguageFilter Top Repos was actually filtered to when
 * this repo was tapped) is threaded down through DiscoverNavHost and
 * RepoIssuesScreen rather than re-derived from repoGroup.issues'
 * display strings — needed as an exact LanguageFilter value both for
 * the fetch's own `language:` query qualifier and to address the
 * correct (language, mode) cache entry for write-back; a repo's
 * display language string alone isn't reliably reversible back to one.
 *
 * WORTH BEING HONEST ABOUT: 100 covers the overwhelming majority of
 * real repos for a single label, but isn't an absolute guarantee of
 * completeness for a repo with more than 100 open matches on one
 * label — genuinely rare for "good first issue" or "help wanted"
 * specifically, and deliberately not chased further with a second
 * pagination system layered on top of this screen too. If that ever
 * turns out to matter in practice, extending this with its own Load
 * More is a bounded, well-understood follow-up (Top Repos already
 * proves the pattern) — not attempted here on spec alone.
 *
 * This is a genuinely different cost tradeoff than Top Repos ever had
 * to make, and deliberately NOT automatic — see refreshThisRepo()
 * and RepoIssuesScreen's own doc comments for why it's an explicit
 * button with a visible cost note, not something that just happens.
 *
 * Why a singleton: the state has to live somewhere that outlives any one
 * composition. open() starts a fresh visit to the screen (a new repo):
 * it clears the state and cancels a refresh still running from the
 * previous visit, which is what constructing a new ViewModel for each
 * visit used to do. loadedForRepoFullName exists only so the screen's
 * repeated loadIfNeeded() calls do not reassign issues the service
 * already holds. Nothing here needs a Context: the cache work lives in
 * RepoIssueGroupRepository, which does its own file I/O. The work runs in
 * the shared [AppScope]. Tests build an instance over fakes and install
 * it with [replaceInstanceForTests].
 */
class RepoIssuesService(
    private val modeRepository: IssueModeRepository = IssueModeRepository.instance,
    private val cacheRepository: RepoIssueGroupRepository = RepoIssueGroupRepository.instance,
    private val searchRepository: IssueSearchRepository = IssueSearchRepository.instance,
    private val tokenRepository: ApiTokenRepository = ApiTokenRepository.instance,
    private val scope: CoroutineScope = AppScope.scope,
) {

    private val _state = MutableStateFlow(RepoIssuesState())
    val state: StateFlow<RepoIssuesState> = _state.asStateFlow()

    /** The active Issue Mode, or null while none is chosen (which DiscoverNavHost prevents from ever being observed here). */
    val mode: StateFlow<IssueLabelFilter?> = modeRepository.mode

    private var loadedForRepoFullName: String? = null

    /** Parent of everything one visit to the screen starts; open() cancels it and starts a new one. */
    private var visitJob: Job = SupervisorJob()

    /** Starts a fresh visit: clears the state and cancels a refresh left over from the previous visit. */
    fun open() {
        visitJob.cancel()
        visitJob = SupervisorJob()
        loadedForRepoFullName = null
        _state.value = RepoIssuesState()
    }

    fun loadIfNeeded(repoGroup: RepoIssueGroup) {
        if (loadedForRepoFullName == repoGroup.repoFullName) return
        loadedForRepoFullName = repoGroup.repoFullName
        _state.update { it.copy(issues = repoGroup.issues) }
    }

    /**
     * The one deliberate, explicit exception to this screen's
     * otherwise-zero network calls — see the class comment for the
     * full reasoning. A no-op if a refresh is already in flight — unlike
     * v0.46's checkForMoreIssues(), NOT gated on hasRefreshed, since
     * this is meant to be re-tappable.
     */
    fun refreshThisRepo(repoGroup: RepoIssueGroup, language: LanguageFilter) {
        if (_state.value.isRefreshing) return
        if (tokenRepository.effectiveToken.value.isBlank()) return

        // Defensive fallback only — DiscoverNavHost guarantees a mode
        // is chosen before this screen is ever reachable. See
        // IssueModeRepository.
        val activeMode = resolveActiveMode(modeRepository.mode.value)

        scope.launch(visitJob) {
            _state.update { it.copy(isRefreshing = true, refreshFailure = null) }
            try {
                when (val result = searchRepository.searchRepoIssues(repoGroup.repoFullName, activeMode, language)) {
                    is IssueSearchResult.Success -> {
                        val completeIssues = result.page.issues
                        val refreshedAtMs = System.currentTimeMillis()
                        _state.update {
                            it.copy(
                                issues = completeIssues,
                                isRefreshing = false,
                                hasRefreshed = true,
                                lastRefreshedAtMs = refreshedAtMs,
                            )
                        }
                        cacheRepository.replaceRepoGroup(
                            language = language,
                            mode = activeMode,
                            repoFullName = repoGroup.repoFullName,
                            updatedGroup = if (completeIssues.isEmpty()) null else repoGroup.copy(issues = completeIssues),
                        )
                    }
                    is IssueSearchResult.HttpError -> _state.update {
                        it.copy(isRefreshing = false, refreshFailure = SearchFailure.Http(result.code))
                    }
                    IssueSearchResult.NoToken -> _state.update { it.copy(isRefreshing = false) }
                    is IssueSearchResult.Failed -> showRefreshFailure(result.message)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showRefreshFailure(e.message)
            }
        }
    }

    private fun showRefreshFailure(detail: String?) {
        _state.update { it.copy(isRefreshing = false, refreshFailure = SearchFailure.Failed(detail)) }
    }

    companion object {
        @Volatile
        private var installed: RepoIssuesService? = null

        /** The app-wide service, built from the real repositories on first use. */
        val instance: RepoIssuesService
            get() = installed ?: synchronized(this) {
                installed ?: RepoIssuesService().also { installed = it }
            }

        /**
         * For tests only: installs [service] as the instance (or, with
         * null, drops it so the next access builds a fresh real one).
         * Tests call this in setup, and with null in teardown.
         */
        fun replaceInstanceForTests(service: RepoIssuesService?) {
            installed = service
        }
    }
}
