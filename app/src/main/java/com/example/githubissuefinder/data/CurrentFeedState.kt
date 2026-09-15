package com.example.githubissuefinder.data

import com.example.githubissuefinder.model.Issue
import com.example.githubissuefinder.model.IssueLabelFilter
import com.example.githubissuefinder.model.LanguageFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Plain in-memory singleton — same temporary, not-Hilt-managed pattern as
 * IssueCache/RateLimitState/GitHubApiClient. Holds only the MOST RECENT
 * successful search results from FeedViewModel, replaced wholesale on
 * every new search.
 *
 * totalCount and the active filter selection were added in this Commit
 * Milestone so Insights can show honest context about what it's
 * displaying: GitHub's Search API caps each response at per_page=30
 * items, so issues.size alone can make two very different searches look
 * identical (e.g. both showing "30") when the true totals are wildly
 * different. totalCount is GitHub's own reported match count, read
 * directly from the API response, not derived from what we happened to
 * fetch.
 */
data class FeedSnapshot(
    val issues: List<Issue> = emptyList(),
    val isSampleData: Boolean = false,
    val totalCount: Int = 0,
    val selectedLabel: IssueLabelFilter = IssueLabelFilter.GOOD_FIRST_ISSUE,
    val selectedLanguage: LanguageFilter = LanguageFilter.KOTLIN,
)

object CurrentFeedState {

    private val _state = MutableStateFlow(FeedSnapshot())
    val state: StateFlow<FeedSnapshot> = _state.asStateFlow()

    fun update(
        issues: List<Issue>,
        isSampleData: Boolean,
        totalCount: Int,
        selectedLabel: IssueLabelFilter,
        selectedLanguage: LanguageFilter,
    ) {
        _state.value = FeedSnapshot(issues, isSampleData, totalCount, selectedLabel, selectedLanguage)
    }
}
