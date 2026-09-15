package com.example.githubissuefinder.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.githubissuefinder.data.CurrentFeedState
import com.example.githubissuefinder.data.IssueCache
import com.example.githubissuefinder.data.RateLimitInfo
import com.example.githubissuefinder.data.RateLimitState
import com.example.githubissuefinder.data.SampleIssues
import com.example.githubissuefinder.data.toIssue
import com.example.githubissuefinder.model.IssueLabelFilter
import com.example.githubissuefinder.model.LanguageFilter
import com.example.githubissuefinder.network.GitHubApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * First real ViewModel in this project. Owns the Feed screen's state and
 * talks to GitHubApiClient directly (a plain singleton, not injected) —
 * a Repository abstraction and Hilt-based DI are still future Commit
 * Milestones; this is a deliberate, honest intermediate step.
 *
 * When no GitHub token is configured (GitHubApiClient.hasToken == false),
 * search() skips the network entirely and filters SampleIssues.kt locally
 * — see v0.10 for details. RateLimitState is explicitly cleared to null
 * in that branch, since a rate limit isn't a meaningful concept when
 * nothing is actually being fetched.
 */
class FeedViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    init {
        search(uiState.value.selectedLabel, uiState.value.selectedLanguage)
    }

    fun onLabelSelected(label: IssueLabelFilter) {
        _uiState.update { it.copy(selectedLabel = label) }
        search(label, uiState.value.selectedLanguage)
    }

    fun onLanguageSelected(language: LanguageFilter) {
        _uiState.update { it.copy(selectedLanguage = language) }
        search(uiState.value.selectedLabel, language)
    }

    fun retry() {
        search(uiState.value.selectedLabel, uiState.value.selectedLanguage)
    }

    private fun search(label: IssueLabelFilter, language: LanguageFilter) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            if (!GitHubApiClient.hasToken) {
                val issues = filterSampleIssues(label, language)
                IssueCache.putAll(issues)
                CurrentFeedState.update(issues, isSampleData = true)
                RateLimitState.update(null)
                _uiState.update {
                    it.copy(issues = issues, isLoading = false, isSampleData = true)
                }
                return@launch
            }

            val query = buildString {
                append("label:\"${label.queryValue}\"")
                append(" state:open")
                language.queryValue?.let { append(" language:$it") }
            }

            try {
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

                val remaining = response.headers()["X-RateLimit-Remaining"]?.toIntOrNull()
                val limit = response.headers()["X-RateLimit-Limit"]?.toIntOrNull()
                RateLimitState.update(
                    if (remaining != null && limit != null) RateLimitInfo(remaining, limit) else null,
                )

                val issues = body.items.map { it.toIssue(language.queryValue) }
                IssueCache.putAll(issues)
                CurrentFeedState.update(issues, isSampleData = false)
                _uiState.update {
                    it.copy(issues = issues, isLoading = false, isSampleData = false)
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
     * Applies the same label/language qualifiers a live search would send
     * to GitHub, but locally against the fixed sample set. Depending on
     * the selection, this can legitimately return few or zero results —
     * that's expected, not a bug, since SampleIssues.kt is only 5 fixed
     * issues rather than GitHub's full corpus.
     */
    private fun filterSampleIssues(label: IssueLabelFilter, language: LanguageFilter) =
        SampleIssues.all.filter { issue ->
            val matchesLabel = issue.labels.any { it.name.equals(label.queryValue, ignoreCase = true) }
            val matchesLanguage = language.queryValue == null ||
                issue.language?.equals(language.queryValue, ignoreCase = true) == true
            matchesLabel && matchesLanguage
        }
}
