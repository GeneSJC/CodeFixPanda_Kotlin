package com.example.githubissuefinder.data

import com.example.githubissuefinder.model.Issue
import com.example.githubissuefinder.model.RepoIssueGroup

/** What updateIssuesInRepoGroups() below needs to report back to its caller — see that function's own doc comment. repoGroups is the complete updated list; newRepoCount/newIssueCount are counted DURING the update, not recomputed afterward, so they can't drift from what actually changed. */
data class RepoGroupUpdate(
    val repoGroups: List<RepoIssueGroup>,
    val newRepoCount: Int,
    val newIssueCount: Int,
)

/**
 * v0.49 ("Cache-Preserving Refresh"): addIssuesToRepoGroups()'s
 * counterpart for TopReposService.checkForNewRepos() — see that
 * function's own doc comment for why a separate function rather than a
 * flag on addIssuesToRepoGroups() itself. The key difference: where
 * addIssuesToRepoGroups() SKIPS an issue whose id is already present (correct
 * for Load More — the same issue genuinely showing up again across two
 * page fetches shouldn't be duplicated, and the copy already on screen
 * is just as current as a re-fetch would be), this REPLACES it with the
 * freshly-fetched copy, since the whole point here is picking up
 * changes (comment count, labels, updated time) to something already
 * on screen.
 *
 * A standalone, pure top-level function (not a private method on
 * TopReposViewModel, which is what it was originally written as) —
 * moved out specifically so it's directly unit-testable with plain
 * JUnit: TopReposViewModel extends AndroidViewModel, which needs a real
 * or mocked Application to construct, and this project has neither
 * Mockito nor Robolectric as a test dependency. This function touches
 * nothing Android-specific, so it doesn't need either.
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
 * signal (see TopReposService's class comment for why this replaces the old
 * composite score). No artificial cap on the number of groups
 * shown — whatever groups form from the page(s) fetched so far are
 * all shown, which bounds card count naturally without a separate
 * constant to keep in sync.
 */
fun groupByRepo(issues: List<Issue>): List<RepoIssueGroup> =
    addIssuesToRepoGroups(emptyList(), issues)

/**
 * v0.43: the actual cross-page step for Load More — see TopReposService's class comment for
 * the motivating scenario (one repo's matches spread across two
 * pages). Re-keys existing groups by repoFullName in a
 * LinkedHashMap (preserving their current order, so ties in the
 * final sort stay deterministic rather than depending on an
 * unordered HashMap's iteration), folds each new issue into its
 * repo's existing group if one exists (skipping it if that exact
 * issue id is somehow already present — defensive, in case
 * GitHub's result set shifts slightly between two page requests),
 * or starts a new group if this is the first issue seen for that
 * repo, then re-sorts everything by the now-current match count.
 * groupByRepo() above is just this function called with an empty
 * starting list — the very first page is added to nothing.
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
