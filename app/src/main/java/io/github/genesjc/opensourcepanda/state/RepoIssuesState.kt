package io.github.genesjc.opensourcepanda.state

import io.github.genesjc.opensourcepanda.model.Issue
import io.github.genesjc.opensourcepanda.model.SearchFailure

/**
 * The repo-issues data state: what the screen's list and refresh footer
 * are built from, and nothing about how it is shown. RepoIssuesStateHolder is
 * the one place it lives and changes; RepoIssuesViewModel maps it to
 * RepoIssuesUiState (which adds the message text).
 */
data class RepoIssuesState(
    val issues: List<Issue> = emptyList(),
    /** True while the one-shot complete fetch is in flight. The issues already held stay visible; only the footer reflects it. */
    val isRefreshing: Boolean = false,
    /** True once at least one refresh has completed successfully this visit; then [issues] is every open issue GitHub has for the repo and mode, up to one call's page size. */
    val hasRefreshed: Boolean = false,
    /** When the last successful refresh completed. Null until [hasRefreshed] is first true. */
    val lastRefreshedAtMs: Long? = null,
    /** Set when a refresh fails; [issues] is left exactly as it was. */
    val refreshFailure: SearchFailure? = null,
)
