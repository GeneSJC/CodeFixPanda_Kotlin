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
 * fields; they only ever affect the footer.
 *
 * v0.50 ("Refresh this repo" write-back): renamed from the v0.46
 * "Check for More Issues" names to match — isCheckingForMore ->
 * isRefreshing, allIssuesLoaded -> hasRefreshed, checkForMoreError ->
 * refreshError. The rename isn't cosmetic: hasRefreshed no longer means
 * "hide the button forever," it means "a result line has something to
 * report" — the button stays reusable, see RepoIssuesViewModel's
 * refreshThisRepo() doc comment.
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
     * True once at least one refresh has completed successfully this
     * session — at that point `issues` holds EVERY open issue GitHub
     * has for this repo and the active mode as of that refresh (up to
     * the single call's own page-size limit; see
     * RepoIssuesViewModel's doc comment), not just whatever happened to
     * be swept up building the Top Repos card. Drives the footer's
     * result line ("All 7 open issues · checked 2m ago"); does NOT hide
     * the "Refresh this repo" button, which stays available for
     * repeated use.
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
