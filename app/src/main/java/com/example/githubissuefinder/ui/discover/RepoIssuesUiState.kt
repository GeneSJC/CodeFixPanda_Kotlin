package com.example.githubissuefinder.ui.discover

import com.example.githubissuefinder.model.Issue

/**
 * v0.42, REVISED after removing this screen's in-screen label picker:
 * there's no longer any way to view a label other than the app's
 * active Issue Mode, so there's nothing left to track besides the
 * issues themselves. selectedLabel and otherLabelKnownEmpty (which
 * existed to support switching to the OTHER label) are both gone —
 * neither could ever differ from a single fixed value once that
 * affordance doesn't exist. isLoading and errorMessage (in their
 * original, whole-screen-scoped form) are gone too: populating this
 * screen from RepoIssueGroup.issues is a pure, synchronous assignment
 * with no network call — see RepoIssuesViewModel's doc comment for
 * the full reasoning.
 *
 * v0.46 ("Check for More Issues") reintroduces network-call-adjacent
 * state, but scoped narrowly to the optional, explicit completeness
 * check below — NOT a return to a whole-screen isLoading/errorMessage
 * pair. The issues already shown are never gated behind any of these
 * three fields; they only ever affect the footer.
 */
data class RepoIssuesUiState(
    val issues: List<Issue> = emptyList(),
    /**
     * v0.46 ("Check for More Issues"): true while the one-shot complete
     * fetch is in flight. Deliberately separate from anything
     * resembling a full-screen loading state — the issues already
     * shown (from RepoIssueGroup, zero-cost) stay visible the whole
     * time; only the footer reflects this.
     */
    val isCheckingForMore: Boolean = false,
    /**
     * True once the complete fetch has succeeded — at that point
     * `issues` holds EVERY open issue GitHub has for this repo and the
     * active mode (up to the single call's own page-size limit; see
     * RepoIssuesViewModel's doc comment), not just whatever happened
     * to be swept up building the Top Repos card. Drives the footer
     * switching from the "Check for More Issues" button to a plain
     * "Showing all N issues" line — there's nothing left to check for
     * once this is true, so re-showing the button would be misleading.
     */
    val allIssuesLoaded: Boolean = false,
    /**
     * Set when the complete-fetch call fails — deliberately NOT
     * touching `issues`, matching Top Repos' loadMoreError: a failed
     * "Check for More" leaves whatever was already shown exactly as
     * it was; only the footer reflects the failure, with its own retry
     * affordance.
     */
    val checkForMoreError: String? = null,
)
