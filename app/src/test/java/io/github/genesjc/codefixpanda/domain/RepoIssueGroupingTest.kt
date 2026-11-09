package io.github.genesjc.codefixpanda.domain

import io.github.genesjc.codefixpanda.model.Issue
import io.github.genesjc.codefixpanda.model.RepoIssueGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the properties checkForNewRepos() depends on (see TopReposStateHolder's
 * doc comment): a repo/issue count that never shrinks from this update, a
 * new repo getting added, a new issue appending to an existing repo,
 * and an already-present issue's fields getting replaced — the whole
 * reason this function exists instead of reusing addIssuesToRepoGroups(),
 * which deliberately skips an issue whose id is already present rather
 * than refreshing it.
 */
class RepoIssueGroupingTest {

    private fun issue(id: Long, repoOwner: String, repoName: String, commentCount: Int = 0) = Issue(
        id = id,
        title = "Issue $id",
        bodyText = "",
        repoOwner = repoOwner,
        repoName = repoName,
        labels = emptyList(),
        language = null,
        commentCount = commentCount,
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
    fun `empty existing plus new issues groups everything as new`() {
        val result = updateIssuesInRepoGroups(
            existing = emptyList(),
            newIssues = listOf(issue(1, "octocat", "hello-world"), issue(2, "octocat", "hello-world")),
        )
        assertEquals(1, result.repoGroups.size)
        assertEquals(2, result.repoGroups.single().matchCount)
        assertEquals(1, result.newRepoCount)
        assertEquals(2, result.newIssueCount)
    }

    @Test
    fun `a new repo in the fetch is added without touching existing repos`() {
        val existing = listOf(group(issue(1, "octocat", "hello-world")))
        val result = updateIssuesInRepoGroups(
            existing = existing,
            newIssues = listOf(issue(2, "torvalds", "linux")),
        )
        // The core "never shrinks" property: existing repo/issue still present.
        assertEquals(2, result.repoGroups.size)
        assertTrue(result.repoGroups.any { it.repoFullName == "octocat/hello-world" && it.matchCount == 1 })
        assertTrue(result.repoGroups.any { it.repoFullName == "torvalds/linux" && it.matchCount == 1 })
        assertEquals(1, result.newRepoCount)
        assertEquals(1, result.newIssueCount)
    }

    @Test
    fun `a new issue for an already-present repo appends rather than replacing the group`() {
        val existing = listOf(group(issue(1, "octocat", "hello-world")))
        val result = updateIssuesInRepoGroups(
            existing = existing,
            newIssues = listOf(issue(2, "octocat", "hello-world")),
        )
        val updatedGroup = result.repoGroups.single { it.repoFullName == "octocat/hello-world" }
        assertEquals(2, updatedGroup.matchCount)
        assertEquals(setOf(1L, 2L), updatedGroup.issues.map { it.id }.toSet())
        assertEquals(0, result.newRepoCount)
        assertEquals(1, result.newIssueCount)
    }

    @Test
    fun `an already-present issue id is replaced with the freshly-fetched copy, not duplicated`() {
        val existing = listOf(group(issue(1, "octocat", "hello-world", commentCount = 2)))
        val refreshed = issue(1, "octocat", "hello-world", commentCount = 9)
        val result = updateIssuesInRepoGroups(existing = existing, newIssues = listOf(refreshed))

        val updatedGroup = result.repoGroups.single()
        // Same count as before — the fresh copy REPLACED the stale one, it didn't append.
        assertEquals(1, updatedGroup.matchCount)
        assertEquals(9, updatedGroup.issues.single().commentCount)
        assertEquals(0, result.newRepoCount)
        assertEquals(0, result.newIssueCount)
    }

    @Test
    fun `groups are sorted by match count descending after the update`() {
        val existing = listOf(
            group(issue(1, "small", "repo")),
            group(issue(2, "big", "repo"), issue(3, "big", "repo")),
        )
        val result = updateIssuesInRepoGroups(
            existing = existing,
            newIssues = listOf(issue(4, "small", "repo"), issue(5, "small", "repo")),
        )
        assertEquals(listOf("small/repo", "big/repo"), result.repoGroups.map { it.repoFullName })
        assertEquals(3, result.repoGroups.first().matchCount)
    }

    @Test
    fun `a check that finds nothing new reports zero on both counts`() {
        val existing = listOf(group(issue(1, "octocat", "hello-world")))
        val result = updateIssuesInRepoGroups(existing = existing, newIssues = listOf(issue(1, "octocat", "hello-world")))
        assertEquals(0, result.newRepoCount)
        assertEquals(0, result.newIssueCount)
    }

    // ---- addIssuesToRepoGroups / groupByRepo (moved out of TopReposViewModel) ----

    @Test
    fun `groupByRepo groups by repo and ranks by match count`() {
        val groups = groupByRepo(
            listOf(
                issue(1, "a", "small"),
                issue(2, "b", "big"),
                issue(3, "b", "big"),
            ),
        )
        assertEquals(listOf("b/big", "a/small"), groups.map { it.repoFullName })
        assertEquals(listOf(2, 1), groups.map { it.matchCount })
    }

    @Test
    fun `addIssuesToRepoGroups adds a later page's issues to an existing repo and re-ranks`() {
        val page1 = groupByRepo(listOf(issue(1, "a", "one"), issue(2, "b", "two")))

        val repoGroups = addIssuesToRepoGroups(page1, listOf(issue(3, "b", "two"), issue(4, "b", "two")))

        assertEquals(listOf("b/two", "a/one"), repoGroups.map { it.repoFullName })
        assertEquals(3, repoGroups.first().matchCount)
    }

    @Test
    fun `addIssuesToRepoGroups skips an issue id that is already present`() {
        val existing = groupByRepo(listOf(issue(1, "a", "one", commentCount = 3)))

        val repoGroups = addIssuesToRepoGroups(existing, listOf(issue(1, "a", "one", commentCount = 99)))

        assertEquals(1, repoGroups.single().matchCount)
        // Skipped, not replaced: the copy already on screen is kept.
        assertEquals(3, repoGroups.single().issues.single().commentCount)
    }

    @Test
    fun `addIssuesToRepoGroups with nothing new returns the same groups`() {
        val existing = groupByRepo(listOf(issue(1, "a", "one")))

        assertEquals(existing, addIssuesToRepoGroups(existing, emptyList()))
    }

    // ---- canRequestNextPage ----

    @Test
    fun `a short page means there is nothing more to request`() {
        assertFalse(canRequestNextPage(fetchedThisPage = TOP_REPOS_PER_PAGE - 1, justFetchedPage = 1))
    }

    @Test
    fun `a full page early in the results allows another request`() {
        assertTrue(canRequestNextPage(fetchedThisPage = TOP_REPOS_PER_PAGE, justFetchedPage = 1))
    }

    @Test
    fun `no page is requested once the pages fetched reach GitHub's result cap`() {
        // The first page whose end index (page * per-page) reaches the cap: no further page is offered after it.
        val firstPageThatEndsAtCap = (SEARCH_API_RESULT_CAP + TOP_REPOS_PER_PAGE - 1) / TOP_REPOS_PER_PAGE
        assertFalse(canRequestNextPage(fetchedThisPage = TOP_REPOS_PER_PAGE, justFetchedPage = firstPageThatEndsAtCap))
        assertTrue(canRequestNextPage(fetchedThisPage = TOP_REPOS_PER_PAGE, justFetchedPage = firstPageThatEndsAtCap - 1))
    }
}
