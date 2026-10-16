package com.example.githubissuefinder.data

import com.example.githubissuefinder.model.IssueLabelFilter
import com.example.githubissuefinder.model.LanguageFilter
import com.example.githubissuefinder.model.RepoIssueGroup
import com.example.githubissuefinder.testutil.ViewModelFixtures
import com.example.githubissuefinder.testutil.issueDto
import com.example.githubissuefinder.testutil.repoGroupOf
import com.example.githubissuefinder.testutil.searchFailed
import com.example.githubissuefinder.testutil.searchOk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RepoIssuesService against fakes: the real service over real
 * repositories whose storage and GitHub API are in-memory fakes, doing
 * its work immediately on the calling thread. The wording the user reads
 * is covered in RepoIssuesUiMappingTest and RepoIssuesViewModelTest.
 */
class RepoIssuesServiceTest {

    private val gfi = IssueLabelFilter.GOOD_FIRST_ISSUE
    private val kotlin = LanguageFilter.KOTLIN

    /** The service from [f], opened the way constructing the ViewModel opens it. */
    private fun opened(f: ViewModelFixtures): RepoIssuesService = f.repoIssuesService.also { it.open() }

    private fun cacheWith(f: ViewModelFixtures, vararg groups: RepoIssueGroup) {
        f.cacheStorage.data = mapOf(
            cacheKeyFor(kotlin, gfi) to RepoIssueGroupCacheEntry(groups.toList(), 5, 2, true, 777L),
        )
    }

    @Test
    fun loadingAGroupHoldsItsIssues() {
        val service = opened(ViewModelFixtures())
        val group = repoGroupOf("octo", "repo", 1, 2)

        service.loadIfNeeded(group)

        assertEquals(group.issues, service.state.value.issues)
    }

    @Test
    fun loadingTheSameRepoAgainDoesNotReassignTheIssues() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1), issueDto(2), issueDto(3))) }
        val service = opened(f)
        val group = repoGroupOf("octo", "repo", 1)
        service.loadIfNeeded(group)
        service.refreshThisRepo(group, kotlin)

        service.loadIfNeeded(group)

        // The refreshed list is kept, not reset to the group's original issues.
        assertEquals(3, service.state.value.issues.size)
    }

    @Test
    fun refreshingFetchesTheReposIssuesAndHoldsThem() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1), issueDto(2), issueDto(3))) }
        val service = opened(f)
        val group = repoGroupOf("octo", "repo", 1)
        service.loadIfNeeded(group)

        service.refreshThisRepo(group, kotlin)

        val state = service.state.value
        assertEquals(listOf(1L, 2L, 3L), state.issues.map { it.id })
        assertTrue(state.hasRefreshed)
        assertFalse(state.isRefreshing)
        assertNull(state.refreshFailure)
        assertEquals(Triple(repoIssuesQuery("octo/repo", gfi), REPO_ISSUES_MAX_PER_PAGE, 1), f.api.calls.single())
    }

    @Test
    fun refreshingWritesTheUpdatedGroupBackIntoTheCache() {
        val f = ViewModelFixtures()
        val group = repoGroupOf("octo", "repo", 1)
        cacheWith(f, group, repoGroupOf("other", "thing", 9))
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1), issueDto(2), issueDto(3))) }
        val service = opened(f)

        service.refreshThisRepo(group, kotlin)

        val entry = f.cacheStorage.data.getValue(cacheKeyFor(kotlin, gfi))
        assertEquals(3, entry.repoGroups.first { it.repoFullName == "octo/repo" }.issues.size)
        assertEquals(1, entry.repoGroups.first { it.repoFullName == "other/thing" }.issues.size)
        // A single-repo refresh doesn't make the whole list look fresher.
        assertEquals(777L, entry.fetchedAtMs)
    }

    @Test
    fun refreshingARepoThatNowHasNoOpenIssuesRemovesItFromTheCache() {
        val f = ViewModelFixtures()
        val group = repoGroupOf("octo", "repo", 1)
        cacheWith(f, group, repoGroupOf("other", "thing", 9))
        f.api.handler = { _, _, _ -> searchOk(emptyList()) }
        val service = opened(f)

        service.refreshThisRepo(group, kotlin)

        assertEquals(listOf("other/thing"), f.cacheStorage.data.getValue(cacheKeyFor(kotlin, gfi)).repoGroups.map { it.repoFullName })
        assertTrue(service.state.value.issues.isEmpty())
    }

    @Test
    fun anErrorResponseKeepsItsCodeAndLeavesTheIssuesAlone() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> searchFailed(500) }
        val service = opened(f)
        val group = repoGroupOf("octo", "repo", 1, 2)
        service.loadIfNeeded(group)

        service.refreshThisRepo(group, kotlin)

        val state = service.state.value
        assertEquals(SearchFailure.Http(500), state.refreshFailure)
        assertFalse(state.isRefreshing)
        assertEquals(group.issues, state.issues)
    }

    @Test
    fun aFailedRequestKeepsWhatWentWrong() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> throw java.io.IOException("no network") }
        val service = opened(f)

        service.refreshThisRepo(repoGroupOf("octo", "repo", 1), kotlin)

        assertEquals(SearchFailure.Failed("no network"), service.state.value.refreshFailure)
        assertFalse(service.state.value.isRefreshing)
    }

    @Test
    fun withoutATokenRefreshingDoesNothing() {
        val f = ViewModelFixtures(buildToken = "")
        val service = opened(f)

        service.refreshThisRepo(repoGroupOf("octo", "repo", 1), kotlin)

        assertTrue(f.api.calls.isEmpty())
        assertFalse(service.state.value.isRefreshing)
    }

    @Test
    fun openingAgainStartsAFreshVisitWithNothingLeftOver() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1), issueDto(2))) }
        val service = opened(f)
        val group = repoGroupOf("octo", "repo", 1)
        service.loadIfNeeded(group)
        service.refreshThisRepo(group, kotlin)
        assertTrue(service.state.value.hasRefreshed)

        service.open()

        assertEquals(RepoIssuesState(), service.state.value)
        // The same repo can be loaded again after a fresh visit.
        service.loadIfNeeded(group)
        assertEquals(group.issues, service.state.value.issues)
    }
}
