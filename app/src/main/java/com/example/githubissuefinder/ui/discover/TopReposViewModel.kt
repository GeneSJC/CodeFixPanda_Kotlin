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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
 * — see IssueMode.kt. There is no onModeSelected() here anymore: the
 * mode dropdown this screen briefly had was removed in this same CM
 * (see TopReposScreen's doc comment) — Mode is now only ever set from
 * IssueModeGateScreen, once, at the start of a session.
 */
class TopReposViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TopReposUiState())
    val uiState: StateFlow<TopReposUiState> = _uiState.asStateFlow()

    init {
        load(uiState.value.selectedLanguage)
    }

    fun onLanguageSelected(language: LanguageFilter) {
        _uiState.update { it.copy(selectedLanguage = language) }
        load(language)
    }

    fun retry() = load(uiState.value.selectedLanguage)

    private fun load(language: LanguageFilter) {
        if (!GitHubApiClient.hasToken) {
            _uiState.update { it.copy(noTokenConfigured = true, isLoading = false) }
            return
        }

        // Defensive fallback only — DiscoverNavHost never creates this
        // screen (and therefore this ViewModel) while IssueMode.current
        // is null. See IssueMode.kt.
        val activeMode = IssueMode.current ?: IssueLabelFilter.GOOD_FIRST_ISSUE

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, noTokenConfigured = false) }
            try {
                val query = buildString {
                    append("label:\"${activeMode.queryValue}\"")
                    append(" state:open")
                    language.queryValue?.let { append(" language:$it") }
                }
                val response = withContext(Dispatchers.IO) {
                    GitHubApiClient.api.searchIssues(query = query)
                }
                val body = response.body()
                if (!response.isSuccessful || body == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "GitHub returned an error (HTTP ${response.code()})",
                        )
                    }
                    return@launch
                }
                RateLimitState.update(parseRateLimitHeaders(response.headers()))
                val issues = body.items.map { it.toIssue(languageFilter = language.queryValue) }
                _uiState.update {
                    it.copy(
                        repoGroups = groupByRepo(issues),
                        isLoading = false,
                        totalIssueCount = body.totalCount,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Couldn't load issues: ${e.message ?: "unknown error"}",
                    )
                }
            }
        }
    }

    /**
     * Groups already-fetched issues by parent repo and sorts the
     * resulting groups by match count, descending — the v0.42 ranking
     * signal (see class doc comment for why this replaces the old
     * composite score). No artificial cap on the number of groups
     * shown (the old TOP_N = 20 is gone): whatever groups form from
     * this one page of results (GitHub's default 30 per page) are all
     * shown, which bounds card count naturally without a separate
     * constant to keep in sync.
     */
    private fun groupByRepo(issues: List<Issue>): List<RepoIssueGroup> =
        issues
            .groupBy { it.repoFullName }
            .map { (fullName, groupIssues) ->
                val first = groupIssues.first()
                RepoIssueGroup(
                    repoOwner = first.repoOwner,
                    repoName = first.repoName,
                    repoFullName = fullName,
                    repoDisplayTitle = first.repoDisplayTitle,
                    issues = groupIssues,
                )
            }
            .sortedByDescending { it.matchCount }
}
