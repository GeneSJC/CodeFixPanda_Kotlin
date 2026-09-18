package com.example.githubissuefinder.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.githubissuefinder.data.RateLimitState
import com.example.githubissuefinder.data.parseRateLimitHeaders
import com.example.githubissuefinder.data.scoreAndRankRepos
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
 * discussion): loads a candidate pool of Kotlin repos via repository
 * search, scores them via scoreAndRankRepos(), and keeps the top 20.
 *
 * No sample-data fallback yet (see TopReposUiState) — this screen
 * requires a configured GitHub token.
 *
 * Repository search shares the SAME Search API rate-limit bucket as
 * issue search, so this correctly updates RateLimitState too — unlike
 * the Comments endpoint (v0.24), which is a genuinely separate bucket
 * and deliberately does NOT touch RateLimitState.
 */
class TopReposViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TopReposUiState())
    val uiState: StateFlow<TopReposUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        if (!GitHubApiClient.hasToken) {
            _uiState.update { it.copy(noTokenConfigured = true, isLoading = false) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, noTokenConfigured = false) }
            try {
                val response = withContext(Dispatchers.IO) {
                    GitHubApiClient.api.searchRepositories(query = "language:kotlin")
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
