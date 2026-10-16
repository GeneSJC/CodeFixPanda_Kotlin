package com.example.githubissuefinder.ui.discover

import androidx.lifecycle.viewModelScope
import com.example.githubissuefinder.data.ReadmeRepository
import com.example.githubissuefinder.ui.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Loads a repo's README for the README screen. The request, the token
 * check and the decoding all live in ReadmeRepository (see its doc
 * comment, and ReadmeDto's, for why this is a different API/quota from
 * everything else in this app and deliberately not tracked by
 * IssueSearchRepository.rateLimit — per direct instruction: no rate-limit indicator for
 * this screen). This ViewModel only maps what the repository reports to
 * the screen's state (RepoReadmeUiState.toUiState()).
 *
 * The repository is a constructor argument defaulting to the app-wide
 * instance so a test can pass one over a fake; a ViewModel whose
 * constructor arguments all have defaults still has the no-argument
 * constructor the default factory needs. loadedFor* dedup guard follows
 * RepoIssuesViewModel's pattern, so navigating away and back within the
 * same process doesn't redundantly refetch.
 *
 * Rendering itself (Markwon, native TextView — explicitly NOT a
 * WebView, per direct instruction) lives in RepoReadmeScreen; this
 * ViewModel only ever hands it a plain markdown String.
 */
class RepoReadmeViewModel(
    private val repository: ReadmeRepository = ReadmeRepository.instance,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(RepoReadmeUiState())
    val uiState: StateFlow<RepoReadmeUiState> = _uiState.asStateFlow()

    private var loadedForRepoFullName: String? = null

    fun loadIfNeeded(repoOwner: String, repoName: String, repoFullName: String) {
        if (loadedForRepoFullName == repoFullName) return
        loadedForRepoFullName = repoFullName

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, notFound = false) }
            _uiState.value = repository.fetch(repoOwner, repoName).toUiState()
        }
    }
}
