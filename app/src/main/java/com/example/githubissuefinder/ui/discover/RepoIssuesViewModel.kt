package com.example.githubissuefinder.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.githubissuefinder.data.RateLimitState
import com.example.githubissuefinder.data.parseRateLimitHeaders
import com.example.githubissuefinder.data.toIssue
import com.example.githubissuefinder.model.IssueLabelFilter
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
 * this screen will ever ask for. See checkForMoreIssues()'s own doc
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
 * v0.46 ("Check for More Issues") is the one, deliberate exception to
 * that "zero network calls" property — read this before assuming it
 * still holds. repoGroup.issues only ever contains whatever this
 * repo's matches happened to be swept up while Top Repos was building
 * its list (bounded by however far Load More had gotten at the time).
 * A repo with more matching issues than that is a real, confirmed
 * completeness gap — this screen could be showing a partial view
 * without any way to know it. checkForMoreIssues() closes that gap
 * with ONE explicit, user-initiated fetch: `repo:owner/name
 * label:"..." state:open`, scoped to just this repo and the active
 * mode, asking for up to MAX_PER_PAGE (100 — GitHub's own per-call
 * max) results in a single call. On success, this REPLACES `issues`
 * outright with the complete result (not a merge — there's nothing to
 * dedup against, this call already returns everything for this query)
 * and sets allIssuesLoaded, which the screen uses to stop offering the
 * button again.
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
 * to make, and deliberately NOT automatic — see checkForMoreIssues()
 * and RepoIssuesScreen's own doc comments for why it's an explicit
 * button with a visible cost note, not something that just happens.
 *
 * Still a ViewModel, not just a `remember` in the Composable, for
 * consistency with every other screen in this app — loadedForRepoFullName
 * exists purely so navigating away and back within the same process
 * doesn't redundantly reassign issues to the value they already held.
 */
class RepoIssuesViewModel : ViewModel() {

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
     * full reasoning. A no-op if a check is already in flight or the
     * complete set has already been loaded this session, so this is
     * safe to wire directly to a button's onClick.
     *
     * language for the fetched issues' display is taken from whichever
     * issue is already showing (repoGroup.issues.firstOrNull()?.language)
     * rather than passed a fresh value — this call deliberately has no
     * `language:` qualifier of its own (it's scoped by repo, not by
     * language), so there's nothing new to derive it from; reusing what
     * Top Repos' own original fetch already determined keeps newly-
     * fetched cards visually consistent with ones already on screen,
     * rather than showing no language dot on some cards and not others.
     */
    fun checkForMoreIssues(repoGroup: RepoIssueGroup) {
        val state = uiState.value
        if (state.isCheckingForMore || state.allIssuesLoaded) return
        if (!GitHubApiClient.hasToken) return

        // Defensive fallback only — DiscoverNavHost guarantees a mode
        // is chosen before this screen is ever reachable. See IssueMode.kt.
        val activeMode = IssueMode.current ?: IssueLabelFilter.GOOD_FIRST_ISSUE
        val languageLabel = repoGroup.issues.firstOrNull()?.language

        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingForMore = true, checkForMoreError = null) }
            try {
                val query = "repo:${repoGroup.repoFullName} label:\"${activeMode.queryValue}\" state:open"
                val response = withContext(Dispatchers.IO) {
                    GitHubApiClient.api.searchIssues(query = query, perPage = MAX_PER_PAGE)
                }
                val body = response.body()
                if (!response.isSuccessful || body == null) {
                    _uiState.update {
                        it.copy(
                            isCheckingForMore = false,
                            checkForMoreError = "GitHub returned an error (HTTP ${response.code()})",
                        )
                    }
                    return@launch
                }
                RateLimitState.update(parseRateLimitHeaders(response.headers()))
                val completeIssues = body.items.map { it.toIssue(languageFilter = languageLabel) }
                _uiState.update {
                    it.copy(
                        issues = completeIssues,
                        isCheckingForMore = false,
                        allIssuesLoaded = true,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCheckingForMore = false,
                        checkForMoreError = "Couldn't check for more issues: ${e.message ?: "unknown error"}",
                    )
                }
            }
        }
    }
}
