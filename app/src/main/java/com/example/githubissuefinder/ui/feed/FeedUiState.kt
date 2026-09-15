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
    /** True when no GitHub token is configured and results are filtered from SampleIssues.kt instead of a live search. */
    val isSampleData: Boolean = false,
    /**
     * GitHub's own reported total match count for the current search —
     * NOT the same as issues.size, since results are capped at
     * per_page=30. In sample-data mode this equals issues.size, since
     * SampleIssues.kt has no "more beyond what's shown" concept.
     */
    val totalResultCount: Int = 0,
)
