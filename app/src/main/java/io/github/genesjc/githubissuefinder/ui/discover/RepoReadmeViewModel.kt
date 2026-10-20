package io.github.genesjc.githubissuefinder.ui.discover

import androidx.lifecycle.viewModelScope
import io.github.genesjc.githubissuefinder.data.ReadmeRepository
import io.github.genesjc.githubissuefinder.ui.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.inject

/**
 * Loads a repo's README for the README widget. The request, token check and decoding live in
 * ReadmeRepository, which uses a different API and quota from search and is deliberately not shown
 * in the rate-limit chip. This ViewModel maps what the repository reports to the screen's state. A
 * loadedFor* guard avoids refetching when the user navigates away and back within the process.
 * Rendering (Markwon in a native TextView, not a WebView) is the screen's job; this hands it a plain
 * markdown String.
 */
class RepoReadmeViewModel : BaseViewModel() {

    private val repository: ReadmeRepository by inject()

    private val _uiState = MutableStateFlow(RepoReadmeUiState())
    val uiState: StateFlow<RepoReadmeUiState> = _uiState.asStateFlow()

    private var loadedForRepoFullName: String? = null

    fun loadIfNeeded(repoOwner: String, repoName: String, repoFullName: String) {
        if (loadedForRepoFullName == repoFullName) return
        loadedForRepoFullName = repoFullName

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, notFound = false) }
            _uiState.value = repository.fetch(repoOwner, repoName).toUiState(string)
        }
    }
}
