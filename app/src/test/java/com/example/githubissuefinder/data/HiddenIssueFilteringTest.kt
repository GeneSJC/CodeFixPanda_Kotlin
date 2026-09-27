package com.example.githubissuefinder.data

import com.example.githubissuefinder.model.Issue
import com.example.githubissuefinder.model.RepoIssueGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers filterHiddenIssues() — the v0.51 ("Hide Issue") display-time
 * filter both TopReposScreen and RepoIssuesScreen apply. The properties
 * that matter most: hiding never touches the underlying data (a pure
 * filter, not a mutation), a repo group filtered down to zero visible
 * issues drops out entirely, and an empty hidden set is a true no-op
 * (same list back, not just an equal one).
 */
class HiddenIssueFilteringTest {

    private fun issue(id: Long, repoOwner: String = "octocat", repoName: String = "hello-world") = Issue(
        id = id,
        title = "Issue $id",
        bodyText = "",
        repoOwner = repoOwner,
        repoName = repoName,
        labels = emptyList(),
        language = null,
        commentCount = 0,
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
    fun `empty hidden set returns the same groups untouched`() {
        val groups = listOf(group(issue(1)), group(issue(2, "torvalds", "linux")))
        val result = filterHiddenIssues(groups, emptySet())
        assertEquals(groups, result)
    }

    @Test
    fun `hiding one issue out of several in a repo removes just that issue`() {
        val groups = listOf(group(issue(1), issue(2)))
        val result = filterHiddenIssues(groups, setOf(1L))

        assertEquals(1, result.size)
        assertEquals(listOf(2L), result.single().issues.map { it.id })
    }

    @Test
    fun `hiding a repo's only issue drops the whole group`() {
        val untouched = group(issue(2, "torvalds", "linux"))
        val groups = listOf(group(issue(1)), untouched)
        val result = filterHiddenIssues(groups, setOf(1L))

        assertEquals(listOf(untouched), result)
    }

    @Test
    fun `hidden ids for issues not present are a harmless no-op`() {
        val groups = listOf(group(issue(1)))
        val result = filterHiddenIssues(groups, setOf(999L))
        assertEquals(groups, result)
    }

    @Test
    fun `does not mutate the issues in the original groups`() {
        val original = group(issue(1), issue(2))
        val groups = listOf(original)
        filterHiddenIssues(groups, setOf(1L))
        // The source RepoIssueGroup itself is untouched — filterHiddenIssues()
        // never mutates in place, it always returns a new, filtered copy.
        assertEquals(2, original.issues.size)
    }

    @Test
    fun `flat-list variant filters the same way for RepoIssuesScreen's own issue list`() {
        val issues = listOf(issue(1), issue(2), issue(3))
        val result = filterHiddenIssuesFlat(issues, setOf(2L))
        assertEquals(listOf(1L, 3L), result.map { it.id })
    }

    @Test
    fun `flat-list variant with empty hidden set is a no-op`() {
        val issues = listOf(issue(1), issue(2))
        assertTrue(filterHiddenIssuesFlat(issues, emptySet()) === issues)
    }
}
