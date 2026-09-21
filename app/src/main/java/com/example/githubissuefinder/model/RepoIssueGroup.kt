package com.example.githubissuefinder.model

/**
 * A repo, as the repo-first Discover flow now builds it (v0.42): NOT a
 * fetched repository object at all — a group of Issue results that
 * happen to share the same parent repo. Replaces the old Repo model
 * entirely for this flow (see TopReposViewModel's doc comment for the
 * full reasoning on why the whole pipeline changed).
 *
 * Built purely by grouping a List<Issue> by repoFullName — every field
 * here is either copied from one of those already-fetched Issue
 * objects or derived from the group itself. This is the direct
 * consequence of switching to GitHub's Issue Search API as the
 * primary query: it returns issues, each carrying just enough about
 * its parent repo (via repository_url) to identify it, and NOTHING
 * else about that repo — no stars, no forks, no description, no
 * license, no last-updated-at for the REPO itself (as opposed to the
 * issue). A repo that appears here is GUARANTEED to have at least one
 * open issue matching the active Issue Mode, by construction — that
 * was the entire point of this pipeline change: the old
 * "goodFirstIssueCount == 0" dead-end case Repo.kt had to represent
 * (and RepoIssuesScreen had to gray out for) literally cannot happen
 * anymore, so there's no nullable "count not yet known" field to carry
 * here either.
 *
 * issues is deliberately the full List<Issue>, not just a count — see
 * RepoIssuesViewModel, which uses this directly as the repo's issue
 * list for the currently active mode with ZERO additional network
 * calls, rather than re-fetching data this screen already fetched to
 * build the group in the first place.
 */
data class RepoIssueGroup(
    val repoOwner: String,
    val repoName: String,
    val repoFullName: String,
    val repoDisplayTitle: String,
    val issues: List<Issue>,
) {
    /** Number of matching issues in THIS fetched batch — the ranking signal for v0.42 (see TopReposViewModel). */
    val matchCount: Int get() = issues.size
}
