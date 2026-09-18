package com.example.githubissuefinder.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.githubissuefinder.data.RateLimitState
import com.example.githubissuefinder.data.parseRateLimitHeaders
import com.example.githubissuefinder.data.scoreAndRankRepos
import com.example.githubissuefinder.model.LanguageFilter
import com.example.githubissuefinder.network.GitHubApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TOP_N = 20

/**
 * CM 1 of the repo-first "Discover" flow (option C from the architecture
 * discussion): loads a candidate pool of repos for the selected
 * language via repository search, scores them via scoreAndRankRepos(),
 * and keeps the top 20.
 *
 * Reuses the same LanguageFilter enum Feed uses (v0.8), including its
 * ANY option, rather than a second parallel set of language options.
 * GitHub's search endpoint requires a non-empty query, so ANY
 * (queryValue = null) uses "stars:>0" — a qualifier every real repo
 * satisfies — instead of a language restriction, rather than silently
 * falling back to a specific language.
 *
 * Worth knowing: ranking across ANY language will surface extremely
 * popular projects in whatever language happens to dominate GitHub's
 * overall star counts (large JS/Python frameworks, historically) — a
 * real, expected consequence of broadening the pool, not a bug.
 *
 * No sample-data fallback yet (see TopReposUiState) — this screen
 * requires a configured GitHub token.
 *
 * Repository search shares the SAME Search API rate-limit bucket as
 * issue search, so this correctly updates RateLimitState too — unlike
 * the Comments endpoint from the shelved original v0.24, which was a
 * genuinely separate bucket.
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

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, noTokenConfigured = false) }
            try {
                val query = language.queryValue?.let { "language:$it" } ?: "stars:>0"
                val response = withContext(Dispatchers.IO) {
                    GitHubApiClient.api.searchRepositories(query = query)
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
                val ranked = scoreAndRankRepos(body.items).take(TOP_N)
                _uiState.update { it.copy(repos = ranked, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Couldn't load repos: ${e.message ?: "unknown error"}",
                    )
                }
            }
        }
    }
}
