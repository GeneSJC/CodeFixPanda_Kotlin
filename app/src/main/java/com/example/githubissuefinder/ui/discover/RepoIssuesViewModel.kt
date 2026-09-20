package com.example.githubissuefinder.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.githubissuefinder.data.RateLimitState
import com.example.githubissuefinder.data.parseRateLimitHeaders
import com.example.githubissuefinder.data.toIssue
import com.example.githubissuefinder.model.IssueLabelFilter
import com.example.githubissuefinder.model.Repo
import com.example.githubissuefinder.network.GitHubApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The repo-scoped issue list — CM "B" of the Discover redesign
 * (option C from the architecture discussion): the drill-in that
 * every Discover CM since v0.25 has flagged as deferred, since tapping
 * a repo was previously a no-op.
 *
 * Deliberately narrower than Feed:
 *  - NO language filter — redundant once you've already committed to
 *    one specific repo (flagged back in the original architecture
 *    discussion).
 *  - Label filter (good first issue / help wanted) reused via the
 *    newly-shared LabelFilterRow component (v0.38), same enum Feed
 *    uses.
 *  - Tapping an issue opens it directly in GitHub (same pattern used
 *    elsewhere), NOT the existing DetailScreen/Bookmarks machinery —
 *    whether bookmarking should span both flows is a separate,
 *    explicitly deferred question, not decided implicitly here.
 *  - No "no token configured" check: this screen is only reachable by
 *    tapping a repo on Top Repos, which itself requires a token to
 *    show anything — the check would be dead code here.
 *
 * languageFilter passed to toIssue() uses the REPO'S OWN known language
 * (from the Repo model, already fetched when Top Repos loaded) rather
 * than echoing back a search query the way Feed does — more accurate
 * here, since we're not filtering by language at all in this screen.
 *
 * loadIfNeeded() rather than loading in init{} because this ViewModel
 * doesn't know which repo it's for until the Composable tells it — see
 * RepoIssuesScreen's LaunchedEffect.
 */
class RepoIssuesViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RepoIssuesUiState())
    val uiState: StateFlow<RepoIssuesUiState> = _uiState.asStateFlow()

    private var loadedForRepoFullName: String? = null

    fun loadIfNeeded(repo: Repo) {
        if (loadedForRepoFullName == repo.fullName) return
        loadedForRepoFullName = repo.fullName
        load(repo, uiState.value.selectedLabel)
    }

    fun onLabelSelected(repo: Repo, label: IssueLabelFilter) {
        _uiState.update { it.copy(selectedLabel = label) }
        load(repo, label)
    }

    fun retry(repo: Repo) = load(repo, uiState.value.selectedLabel)

    private fun load(repo: Repo, label: IssueLabelFilter) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val query = "repo:${repo.ownerLogin}/${repo.name} label:\"${label.queryValue}\" state:open"
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
                val issues = body.items.map { it.toIssue(languageFilter = repo.language) }
                _uiState.update { it.copy(issues = issues, isLoading = false) }
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
}
