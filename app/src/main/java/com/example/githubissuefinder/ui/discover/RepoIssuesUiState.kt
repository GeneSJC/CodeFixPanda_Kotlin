package com.example.githubissuefinder.ui.discover

import com.example.githubissuefinder.model.Issue
import com.example.githubissuefinder.model.IssueLabelFilter

data class RepoIssuesUiState(
    val issues: List<Issue> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedLabel: IssueLabelFilter = IssueLabelFilter.GOOD_FIRST_ISSUE,
)
