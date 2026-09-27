package com.example.githubissuefinder.data

import com.example.githubissuefinder.model.Issue
import com.example.githubissuefinder.model.RepoIssueGroup

/** What mergeReplacingByIssueId() below needs to report back to its caller — see that function's own doc comment. groups is the merged result; newRepoCount/newIssueCount are counted DURING the merge, not recomputed afterward, so they can't drift from what actually changed. */
data class ReplacingMergeResult(
    val groups: List<RepoIssueGroup>,
    val newRepoCount: Int,
    val newIssueCount: Int,
)

/**
 * v0.49 ("Cache-Preserving Refresh"): TopReposViewModel's
 * mergeIntoGroups()'s counterpart for checkForNewRepos() — see that
 * function's own doc comment for why a separate function rather than a
 * flag on mergeIntoGroups() itself. The key difference: where
 * mergeIntoGroups() SKIPS an issue whose id is already present (correct
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
fun mergeReplacingByIssueId(existing: List<RepoIssueGroup>, newIssues: List<Issue>): ReplacingMergeResult {
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
    return ReplacingMergeResult(
        groups = byRepo.values.sortedByDescending { it.matchCount },
        newRepoCount = newRepoCount,
        newIssueCount = newIssueCount,
    )
}
