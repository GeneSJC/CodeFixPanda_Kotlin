package com.example.githubissuefinder.ui.feed

import com.example.githubissuefinder.model.Issue
import com.example.githubissuefinder.model.IssueLabelFilter
import com.example.githubissuefinder.model.LanguageFilter

data class FeedUiState(
    val issues: List<Issue> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedLabel: IssueLabelFilter = IssueLabelFilter.GOOD_FIRST_ISSUE,
    val selectedLanguage: LanguageFilter = LanguageFilter.KOTLIN,
)
