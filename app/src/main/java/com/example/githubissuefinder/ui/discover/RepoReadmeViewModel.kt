package com.example.githubissuefinder.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.githubissuefinder.network.GitHubApiClient
import com.example.githubissuefinder.network.decodeGitHubContentsBase64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Fetches and decodes a repo's README via GitHub's Contents API
 * (GitHubApiService.getReadme) — see that method's and ReadmeDto's own
 * doc comments for why this is a different API/quota from everything
 * else in this app, and deliberately not tracked by RateLimitState
 * (per direct instruction: no rate-limit indicator for this screen).
 *
 * A plain ViewModel, not AndroidViewModel, same reasoning as
 * RepoIssuesViewModel: nothing here needs a Context. loadedFor* dedup
 * guard follows that same file's own pattern, so navigating away and
 * back within the same process doesn't redundantly refetch.
 *
 * Rendering itself (Markwon, native TextView — explicitly NOT a
 * WebView, per direct instruction) lives in RepoReadmeScreen; this
 * ViewModel only ever hands it a plain markdown String.
 */
class RepoReadmeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RepoReadmeUiState())
    val uiState: StateFlow<RepoReadmeUiState> = _uiState.asStateFlow()

    private var loadedForRepoFullName: String? = null

    fun loadIfNeeded(repoOwner: String, repoName: String, repoFullName: String) {
        if (loadedForRepoFullName == repoFullName) return
        loadedForRepoFullName = repoFullName

        if (!GitHubApiClient.hasToken) {
            _uiState.update {
                it.copy(isLoading = false, errorMessage = "A GitHub token is required to load a README.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, notFound = false) }
            try {
                val response = withContext(Dispatchers.IO) {
                    GitHubApiClient.api.getReadme(repoOwner, repoName)
                }
                if (response.code() == 404) {
                    _uiState.update { it.copy(isLoading = false, notFound = true) }
                    return@launch
                }
                val body = response.body()
                if (!response.isSuccessful || body == null) {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "GitHub returned an error (HTTP ${response.code()})")
                    }
                    return@launch
                }
                val markdown = decodeGitHubContentsBase64(body.content, body.encoding)
                if (markdown == null) {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Couldn't read this README's content.")
                    }
                    return@launch
                }
                _uiState.update { it.copy(isLoading = false, markdown = markdown) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Couldn't load the README: ${e.message ?: "unknown error"}")
                }
            }
        }
    }
}
