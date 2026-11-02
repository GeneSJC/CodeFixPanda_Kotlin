package io.github.genesjc.opensourcepanda.ui.repoissues

import io.github.genesjc.opensourcepanda.model.Issue

/**
 * The repo screen's state. There is nothing to track beyond the issues themselves (the mode is
 * fixed, and the issues come from the repo's group with no network call), plus a few footer-only
 * fields for the explicit "Refresh this repo". They never gate the issues already shown.
 */
data class RepoIssuesUiState(
    val issues: List<Issue> = emptyList(),
    /**
     * True while the one-shot complete fetch is in flight. Deliberately
     * separate from anything resembling a full-screen loading state —
     * the issues already shown (from RepoIssueGroup, zero-cost, or a
     * previous refresh) stay visible the whole time; only the footer
     * reflects this.
     */
    val isRefreshing: Boolean = false,
    /**
     * True once a refresh has completed successfully this visit; then `issues` holds every open issue
     * GitHub has for the repo and mode (up to one call's page size). Drives the footer's result line;
     * the "Refresh this repo" button stays available.
     */
    val hasRefreshed: Boolean = false,
    /** When the last successful refresh completed — drives the result line's "checked X ago". Null until hasRefreshed is first true. */
    val lastRefreshedAtMs: Long? = null,
    /**
     * Set when a refresh fails — deliberately NOT touching `issues`,
     * matching Top Repos' loadMoreError: a failed refresh leaves
     * whatever was already shown exactly as it was; only the footer
     * reflects the failure, with its own retry affordance.
     */
    val refreshError: String? = null,
)
