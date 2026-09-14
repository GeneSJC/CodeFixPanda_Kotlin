package com.example.githubissuefinder.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.githubissuefinder.data.IssueCache
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

            val query = buildString {
                append("label:\"${label.queryValue}\"")
                append(" state:open")
                language.queryValue?.let { append(" language:$it") }
            }

            try {
                val response = withContext(Dispatchers.IO) {
                    GitHubApiClient.api.searchIssues(query = query)
                }
                val issues = response.items.map { it.toIssue(language.queryValue) }
                IssueCache.putAll(issues)
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
