package com.example.githubissuefinder.data

import com.example.githubissuefinder.model.Issue
import com.example.githubissuefinder.model.RepoIssueGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers applyRepoGroupReplacement() (RepoIssueGroupRepository.kt) — the pure
 * list-transform behind replaceRepoGroup(), the v0.50 write-back
 * RepoIssuesViewModel.refreshThisRepo() uses so a repo refreshed from
 * its own screen updates the Top Repos cache entry it came from. Two
 * properties matter most: replacing a repo's group leaves every OTHER
 * repo's group untouched, and updatedGroup == null actually removes
 * the repo (the "zero open issues left, drop off Top Repos" case).
 */
class RepoIssueGroupCacheReplacementTest {

    private fun issue(id: Long, repoOwner: String, repoName: String) = Issue(
        id = id,
        title = "Issue $id",
        bodyText = "",
        repoOwner = repoOwner,
        repoName = repoName,
        labels = emptyList(),
        language = null,
        commentCount = 0,
        updatedAtMs = null,
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
    fun `replacing a repo's group leaves other repos untouched`() {
        val untouched = group(issue(1, "octocat", "hello-world"), issue(2, "octocat", "hello-world"))
        val existing = listOf(untouched, group(issue(3, "torvalds", "linux")))

        val refreshedLinux = group(issue(3, "torvalds", "linux"), issue(4, "torvalds", "linux"))
        val result = applyRepoGroupReplacement(existing, "torvalds/linux", refreshedLinux)

        assertEquals(2, result.size)
        assertTrue(result.contains(untouched))
        val linux = result.single { it.repoFullName == "torvalds/linux" }
        assertEquals(2, linux.matchCount)
    }

    @Test
    fun `a null updatedGroup removes the repo entirely`() {
        val existing = listOf(
            group(issue(1, "octocat", "hello-world")),
            group(issue(2, "torvalds", "linux")),
        )
        val result = applyRepoGroupReplacement(existing, "torvalds/linux", null)

        assertEquals(1, result.size)
        assertEquals("octocat/hello-world", result.single().repoFullName)
    }

    @Test
    fun `replacing a repo not currently present just adds it`() {
        val existing = listOf(group(issue(1, "octocat", "hello-world")))
        val newRepo = group(issue(2, "torvalds", "linux"), issue(3, "torvalds", "linux"))
        val result = applyRepoGroupReplacement(existing, "torvalds/linux", newRepo)

        assertEquals(2, result.size)
        assertTrue(result.any { it.repoFullName == "torvalds/linux" && it.matchCount == 2 })
    }

    @Test
    fun `removing a repo that was never present is a harmless no-op`() {
        val existing = listOf(group(issue(1, "octocat", "hello-world")))
        val result = applyRepoGroupReplacement(existing, "nobody/nothing", null)
        assertEquals(existing, result)
    }

    @Test
    fun `result stays sorted by match count descending after replacement`() {
        val existing = listOf(
            group(issue(1, "small", "repo")),
            group(issue(2, "big", "repo"), issue(3, "big", "repo")),
        )
        // "small/repo" picks up two more matches, overtaking "big/repo".
        val grownSmall = group(
            issue(1, "small", "repo"),
            issue(4, "small", "repo"),
            issue(5, "small", "repo"),
        )
        val result = applyRepoGroupReplacement(existing, "small/repo", grownSmall)

        assertEquals(listOf("small/repo", "big/repo"), result.map { it.repoFullName })
    }
}
