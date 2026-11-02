package io.github.genesjc.opensourcepanda.domain

import io.github.genesjc.opensourcepanda.model.Issue
import io.github.genesjc.opensourcepanda.model.RepoIssueGroup

/** What updateIssuesInRepoGroups() below needs to report back to its caller — see that function's own doc comment. repoGroups is the complete updated list; newRepoCount/newIssueCount are counted DURING the update, not recomputed afterward, so they can't drift from what actually changed. */
data class RepoGroupUpdate(
    val repoGroups: List<RepoIssueGroup>,
    val newRepoCount: Int,
    val newIssueCount: Int,
)

/**
 * The counterpart of addIssuesToRepoGroups() for checkForNewRepos(). Where that one skips an issue
 * whose id is already present (right for Load More, where the copy on screen is as current as a
 * re-fetch), this replaces it with the freshly fetched copy, since the point is to pick up changes
 * (comment count, labels, updated time). A pure top-level function so it is unit-testable directly.
 */
fun updateIssuesInRepoGroups(existing: List<RepoIssueGroup>, newIssues: List<Issue>): RepoGroupUpdate {
    val byRepo = LinkedHashMap<String, RepoIssueGroup>()
    existing.forEach { byRepo[it.repoFullName] = it }
    val preexistingRepoNames = byRepo.keys.toSet()
    var newIssueCount = 0
    newIssues.forEach { issue ->
        val current = byRepo[issue.repoFullName]
        if (current == null) {
            byRepo[issue.repoFullName] = RepoIssueGroup(
                repoOwner = issue.repoOwner,
                repoName = issue.repoName,
                repoFullName = issue.repoFullName,
                repoDisplayTitle = issue.repoDisplayTitle,
                issues = listOf(issue),
            )
            newIssueCount++
            return@forEach
        }
        val existingIndex = current.issues.indexOfFirst { it.id == issue.id }
        if (existingIndex == -1) {
            byRepo[issue.repoFullName] = current.copy(issues = current.issues + issue)
            newIssueCount++
        } else {
            byRepo[issue.repoFullName] = current.copy(
                issues = current.issues.toMutableList().apply { set(existingIndex, issue) },
            )
        }
    }
    val newRepoCount = byRepo.keys.count { it !in preexistingRepoNames }
    return RepoGroupUpdate(
        repoGroups = byRepo.values.sortedByDescending { it.matchCount },
        newRepoCount = newRepoCount,
        newIssueCount = newIssueCount,
    )
}

/**
 * Groups already-fetched issues by parent repo and sorts the
 * resulting groups by match count, descending — the v0.42 ranking
 * signal (see TopReposStateHolder's class comment for why this replaces the old
 * composite score). No artificial cap on the number of groups
 * shown — whatever groups form from the page(s) fetched so far are
 * all shown, which bounds card count naturally without a separate
 * constant to keep in sync.
 */
fun groupByRepo(issues: List<Issue>): List<RepoIssueGroup> =
    addIssuesToRepoGroups(emptyList(), issues)

/**
 * The cross-page step for Load More. Re-keys the existing groups by repoFullName in a
 * LinkedHashMap (so ties in the final sort stay deterministic), folds each new issue into its
 * repo's group or starts a new one (skipping an issue id already present, in case GitHub's results
 * shift between page requests), then re-sorts by the new match counts. groupByRepo() is this
 * function called with an empty starting list.
 */
fun addIssuesToRepoGroups(existing: List<RepoIssueGroup>, newIssues: List<Issue>): List<RepoIssueGroup> {
    val byRepo = LinkedHashMap<String, RepoIssueGroup>()
    existing.forEach { byRepo[it.repoFullName] = it }
    newIssues.forEach { issue ->
        val current = byRepo[issue.repoFullName]
        if (current == null) {
            byRepo[issue.repoFullName] = RepoIssueGroup(
                repoOwner = issue.repoOwner,
                repoName = issue.repoName,
                repoFullName = issue.repoFullName,
                repoDisplayTitle = issue.repoDisplayTitle,
                issues = listOf(issue),
            )
        } else if (current.issues.none { it.id == issue.id }) {
            byRepo[issue.repoFullName] = current.copy(issues = current.issues + issue)
        }
    }
    return byRepo.values.sortedByDescending { it.matchCount }
}
