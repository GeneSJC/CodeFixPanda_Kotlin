package io.github.genesjc.githubissuefinder.model

/**
 * A repo as the Discover flow builds it: not a fetched repository, but a group of issues that share
 * a parent repo, built by grouping a List<Issue> by repoFullName. An issue-search response carries
 * only enough about the parent repo to identify it (no stars, forks, description or last-updated),
 * so that is all this holds. A repo here is guaranteed, by construction, to have at least one open
 * issue matching the active mode. [issues] is the full list, which the repo screen uses directly
 * with no further network call.
 */
data class RepoIssueGroup(
    val repoOwner: String,
    val repoName: String,
    val repoFullName: String,
    val repoDisplayTitle: String,
    val issues: List<Issue>,
) {
    /** Number of matching issues in THIS fetched batch — the ranking signal for v0.42 (see TopReposStateHolder). */
    val matchCount: Int get() = issues.size
}
