package com.example.githubissuefinder.data

import com.example.githubissuefinder.model.Issue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Plain in-memory singleton — same temporary, not-Hilt-managed pattern as
 * GitHubApiClient and IssueCache. Holds only the MOST RECENT successful
 * search results from FeedViewModel, replaced wholesale on every new
 * search.
 *
 * Deliberately distinct from IssueCache, which accumulates every issue
 * ever seen (keyed by id) so Detail/Bookmarks can look up anything
 * regardless of current filters. This holds "what Feed is showing right
 * now" — which is what Insights should visualize — not "everything ever
 * loaded."
 *
 * isSampleData lets Insights show the same "showing sample data" context
 * Feed does, rather than silently displaying sample-derived numbers as if
 * they were live.
 */
data class FeedSnapshot(
    val issues: List<Issue> = emptyList(),
    val isSampleData: Boolean = false,
)

object CurrentFeedState {

    private val _state = MutableStateFlow(FeedSnapshot())
    val state: StateFlow<FeedSnapshot> = _state.asStateFlow()

    fun update(issues: List<Issue>, isSampleData: Boolean) {
        _state.value = FeedSnapshot(issues, isSampleData)
    }
}
