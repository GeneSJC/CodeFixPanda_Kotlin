package com.example.githubissuefinder.ui.discover

import com.example.githubissuefinder.model.Issue

/**
 * v0.42, REVISED after removing this screen's in-screen label picker:
 * there's no longer any way to view a label other than the app's
 * active Issue Mode, so there's nothing left to track besides the
 * issues themselves. selectedLabel and otherLabelKnownEmpty (which
 * existed to support switching to the OTHER label) are both gone —
 * neither could ever differ from a single fixed value once that
 * affordance doesn't exist. isLoading and errorMessage are gone too:
 * populating this screen is now a pure, synchronous assignment from
 * RepoIssueGroup.issues (already fetched to build the group in the
 * first place) — there is no network call left anywhere on this
 * screen's path, so nothing here can ever be "loading" or "errored."
 * See RepoIssuesViewModel's doc comment for the full reasoning.
 */
data class RepoIssuesUiState(
    val issues: List<Issue> = emptyList(),
)
