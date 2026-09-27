package com.example.githubissuefinder.data

import com.example.githubissuefinder.model.Issue
import com.example.githubissuefinder.model.RepoIssueGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the exact properties the v0.49 "Cache-Preserving Refresh" plan
 * cares about for checkForNewRepos() (see TopReposViewModel's own doc
 * comment): a repo/issue count that never shrinks from this merge, a
 * new repo getting added, a new issue appending to an existing repo,
 * and an already-present issue's fields getting replaced — the whole
 * reason this function exists instead of reusing mergeIntoGroups(),
 * which deliberately skips an issue whose id is already present rather
 * than refreshing it.
 */
class RepoIssueGroupMergeTest {

    private fun issue(id: Long, repoOwner: String, repoName: String, commentCount: Int = 0) = Issue(
        id = id,
        title = "Issue $id",
        bodyText = "",
        repoOwner = repoOwner,
        repoName = repoName,
        labels = emptyList(),
        language = null,
        commentCount = commentCount,
        updatedAtLabel = "just now",
        htmlUrl = "https://github.com/$repoOwner/$repoName/issues/$id",
    )

    private fun group(vararg issues: Issue) = RepoIssueGroup(
        repoOwner = issues.first().repoOwner,
        repoName = issues.first().repoName,
        repoFullName = issues.first().repoFullName,
        repoDisplayTitle = issues.first().repoDisplayTitle,
        issues = issues.toList(),
    )

    @Test
    fun `empty existing plus new issues groups everything as new`() {
        val result = mergeReplacingByIssueId(
            existing = emptyList(),
            newIssues = listOf(issue(1, "octocat", "hello-world"), issue(2, "octocat", "hello-world")),
        )
        assertEquals(1, result.groups.size)
        assertEquals(2, result.groups.single().matchCount)
        assertEquals(1, result.newRepoCount)
        assertEquals(2, result.newIssueCount)
    }

    @Test
    fun `a new repo in the fetch is added without touching existing repos`() {
        val existing = listOf(group(issue(1, "octocat", "hello-world")))
        val result = mergeReplacingByIssueId(
            existing = existing,
            newIssues = listOf(issue(2, "torvalds", "linux")),
        )
        // The core "never shrinks" property: existing repo/issue still present.
        assertEquals(2, result.groups.size)
        assertTrue(result.groups.any { it.repoFullName == "octocat/hello-world" && it.matchCount == 1 })
        assertTrue(result.groups.any { it.repoFullName == "torvalds/linux" && it.matchCount == 1 })
        assertEquals(1, result.newRepoCount)
        assertEquals(1, result.newIssueCount)
    }

    @Test
    fun `a new issue for an already-present repo appends rather than replacing the group`() {
        val existing = listOf(group(issue(1, "octocat", "hello-world")))
        val result = mergeReplacingByIssueId(
            existing = existing,
            newIssues = listOf(issue(2, "octocat", "hello-world")),
        )
        val merged = result.groups.single { it.repoFullName == "octocat/hello-world" }
        assertEquals(2, merged.matchCount)
        assertEquals(setOf(1L, 2L), merged.issues.map { it.id }.toSet())
        assertEquals(0, result.newRepoCount)
        assertEquals(1, result.newIssueCount)
    }

    @Test
    fun `an already-present issue id is replaced with the freshly-fetched copy, not duplicated`() {
        val existing = listOf(group(issue(1, "octocat", "hello-world", commentCount = 2)))
        val refreshed = issue(1, "octocat", "hello-world", commentCount = 9)
        val result = mergeReplacingByIssueId(existing = existing, newIssues = listOf(refreshed))

        val merged = result.groups.single()
        // Same count as before — the fresh copy REPLACED the stale one, it didn't append.
        assertEquals(1, merged.matchCount)
        assertEquals(9, merged.issues.single().commentCount)
        assertEquals(0, result.newRepoCount)
        assertEquals(0, result.newIssueCount)
    }

    @Test
    fun `groups are sorted by match count descending after merging`() {
        val existing = listOf(
            group(issue(1, "small", "repo")),
            group(issue(2, "big", "repo"), issue(3, "big", "repo")),
        )
        val result = mergeReplacingByIssueId(
            existing = existing,
            newIssues = listOf(issue(4, "small", "repo"), issue(5, "small", "repo")),
        )
        assertEquals(listOf("small/repo", "big/repo"), result.groups.map { it.repoFullName })
        assertEquals(3, result.groups.first().matchCount)
    }

    @Test
    fun `a check that finds nothing new reports zero on both counts`() {
        val existing = listOf(group(issue(1, "octocat", "hello-world")))
        val result = mergeReplacingByIssueId(existing = existing, newIssues = listOf(issue(1, "octocat", "hello-world")))
        assertEquals(0, result.newRepoCount)
        assertEquals(0, result.newIssueCount)
    }
}
