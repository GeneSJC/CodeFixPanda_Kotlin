package com.example.githubissuefinder.ui.discover

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.githubissuefinder.data.BookmarksRepository
import com.example.githubissuefinder.data.HiddenIssuesRepository
import com.example.githubissuefinder.data.IssueModeRepository
import com.example.githubissuefinder.data.RateLimitState
import com.example.githubissuefinder.data.RepoIssueGroupCache
import com.example.githubissuefinder.data.parseRateLimitHeaders
import com.example.githubissuefinder.data.resolveActiveMode
import com.example.githubissuefinder.data.toIssue
import com.example.githubissuefinder.model.Issue
import com.example.githubissuefinder.model.IssueLabelFilter
import com.example.githubissuefinder.model.LanguageFilter
import com.example.githubissuefinder.model.RepoIssueGroup
import com.example.githubissuefinder.network.GitHubApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * GitHub's Search API max page size — the largest single-call result
 * this screen will ever ask for. See refreshThisRepo()'s own doc
 * comment for what this does and doesn't guarantee.
 */
private const val MAX_PER_PAGE = 100

/**
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
 * The direct consequence: for a long time this ViewModel made NO
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
 * repo and the active mode, asking for up to MAX_PER_PAGE (100 —
 * GitHub's own per-call max) results in a single call. On success, this
 * REPLACES `issues` outright with the complete result (not a merge —
 * there's nothing to dedup against, this call already returns
 * everything for this query) — correct here specifically because it IS
 * the complete live set for this one repo, unlike Top Repos' own merge-
 * based checkForNewRepos(), which only ever sees a partial page.
 *
 * UNLIKE v0.46: the button never disappears after use — hasRefreshed
 * drives a small result line ("All 7 open issues · checked 2m ago"),
 * not a one-time switch to a dead end. Re-tappable any number of times
 * this session.
 *
 * WRITE-BACK (new in v0.50): a successful refresh also updates the Top
 * Repos cache entry this repo's group came from
 * (RepoIssueGroupCache.replaceRepoGroup()), so the match count on the
 * Top Repos card the user navigated from agrees on return — without
 * that, this screen's own state could show a repo as having 3 open
 * issues while Top Repos still showed 7 until the next full Top Repos
 * refresh. If the complete fetch comes back EMPTY, that repo is
 * removed from the cache entirely (replaceRepoGroup(updatedGroup =
 * null)) — a repo can't legitimately appear on Top Repos with zero
 * matching issues (see RepoIssueGroup's own doc comment), so a refresh
 * finding none left is exactly the moment for it to leave that list.
 * TopReposViewModel.syncFromCache() is what actually pulls this updated
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
 * Still a ViewModel, not just a `remember` in the Composable, for
 * consistency with every other screen in this app — loadedForRepoFullName
 * exists purely so navigating away and back within the same process
 * doesn't redundantly reassign issues to the value they already held.
 * A plain ViewModel, not AndroidViewModel — Context is taken as a
 * refreshThisRepo() parameter (from LocalContext.current at the call
 * site) rather than pulled in via AndroidViewModel's Application.
 */
class RepoIssuesViewModel(
    private val modeRepository: IssueModeRepository = IssueModeRepository.instance,
    private val hiddenIssuesRepository: HiddenIssuesRepository = HiddenIssuesRepository.instance,
    private val bookmarksRepository: BookmarksRepository = BookmarksRepository.instance,
) : ViewModel() {

    /** Ids of bookmarked issues, to draw each card's star. */
    val bookmarkedIds: StateFlow<Set<Long>> = bookmarksRepository.bookmarkedIds

    /** Toggles [issue]'s bookmark; returns the new state (true = now bookmarked). */
    fun toggleBookmark(issue: Issue): Boolean = bookmarksRepository.toggle(issue)

    /** Ids the user hid, to filter out of the list at display time. */
    val hiddenIds: StateFlow<Set<Long>> = hiddenIssuesRepository.hiddenIds

    fun hideIssue(issue: Issue) {
        hiddenIssuesRepository.hide(issue)
    }

    /** Used by the "Undo" on the "Issue hidden" Snackbar. */
    fun unhideIssue(issueId: Long) {
        hiddenIssuesRepository.unhide(issueId)
    }

    /**
     * The active Issue Mode, re-exposed from IssueModeRepository so
     * RepoIssuesScreen observes it through this ViewModel instead of
     * reading a global. A ViewModel whose constructor arguments all
     * have defaults still has the no-argument constructor viewModel()
     * needs.
     */
    val mode: StateFlow<IssueLabelFilter?> = modeRepository.mode

    private val _uiState = MutableStateFlow(RepoIssuesUiState())
    val uiState: StateFlow<RepoIssuesUiState> = _uiState.asStateFlow()

    private var loadedForRepoFullName: String? = null

    fun loadIfNeeded(repoGroup: RepoIssueGroup) {
        if (loadedForRepoFullName == repoGroup.repoFullName) return
        loadedForRepoFullName = repoGroup.repoFullName
        _uiState.update { it.copy(issues = repoGroup.issues) }
    }

    /**
     * The one deliberate, explicit exception to this screen's
     * otherwise-zero network calls — see class doc comment for the
     * full reasoning. A no-op if a refresh is already in flight — unlike
     * v0.46's checkForMoreIssues(), NOT gated on hasRefreshed, since
     * this is meant to be re-tappable.
     */
    fun refreshThisRepo(context: Context, repoGroup: RepoIssueGroup, language: LanguageFilter) {
        val state = uiState.value
        if (state.isRefreshing) return
        if (!GitHubApiClient.hasToken) return

        // Defensive fallback only — DiscoverNavHost guarantees a mode
        // is chosen before this screen is ever reachable. See
        // IssueModeRepository.
        val activeMode = resolveActiveMode(modeRepository.mode.value)

        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, refreshError = null) }
            try {
                val query = "repo:${repoGroup.repoFullName} label:\"${activeMode.queryValue}\" state:open"
                val response = withContext(Dispatchers.IO) {
                    GitHubApiClient.api.searchIssues(query = query, perPage = MAX_PER_PAGE)
                }
                val body = response.body()
                if (!response.isSuccessful || body == null) {
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            refreshError = "GitHub returned an error (HTTP ${response.code()})",
                        )
                    }
                    return@launch
                }
                RateLimitState.update(parseRateLimitHeaders(response.headers()))
                val completeIssues = body.items.map { it.toIssue(languageFilter = language.queryValue) }
                val refreshedAtMs = System.currentTimeMillis()
                _uiState.update {
                    it.copy(
                        issues = completeIssues,
                        isRefreshing = false,
                        hasRefreshed = true,
                        lastRefreshedAtMs = refreshedAtMs,
                    )
                }
                withContext(Dispatchers.IO) {
                    RepoIssueGroupCache.replaceRepoGroup(
                        context = context,
                        language = language,
                        mode = activeMode,
                        repoFullName = repoGroup.repoFullName,
                        updatedGroup = if (completeIssues.isEmpty()) null else repoGroup.copy(issues = completeIssues),
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        refreshError = "Couldn't refresh this repo: ${e.message ?: "unknown error"}",
                    )
                }
            }
        }
    }
}
