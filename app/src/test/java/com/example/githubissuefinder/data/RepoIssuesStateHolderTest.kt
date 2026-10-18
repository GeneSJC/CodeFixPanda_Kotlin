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
 * RepoIssuesStateHolder against fakes: the real state holder over real
 * repositories whose storage and GitHub API are in-memory fakes, doing
 * its work immediately on the calling thread. The wording the user reads
 * is covered in RepoIssuesUiMappingTest and RepoIssuesViewModelTest.
 */
class RepoIssuesStateHolderTest {

    private val gfi = IssueLabelFilter.GOOD_FIRST_ISSUE
    private val kotlin = LanguageFilter.KOTLIN

    /** The state holder from [f], opened the way constructing the ViewModel opens it. */
    private fun opened(f: ViewModelFixtures): RepoIssuesStateHolder = f.repoIssuesStateHolder.also { it.open() }

    private fun cacheWith(f: ViewModelFixtures, vararg groups: RepoIssueGroup) {
        f.cacheStorage.data = mapOf(
            cacheKeyFor(kotlin, gfi) to RepoIssueGroupCacheEntry(groups.toList(), 5, 2, true, 777L),
        )
    }

    @Test
    fun loadingAGroupHoldsItsIssues() {
        val stateHolder = opened(ViewModelFixtures())
        val group = repoGroupOf("octo", "repo", 1, 2)

        stateHolder.loadIfNeeded(group)

        assertEquals(group.issues, stateHolder.state.value.issues)
    }

    @Test
    fun loadingTheSameRepoAgainDoesNotReassignTheIssues() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1), issueDto(2), issueDto(3))) }
        val stateHolder = opened(f)
        val group = repoGroupOf("octo", "repo", 1)
        stateHolder.loadIfNeeded(group)
        stateHolder.refreshThisRepo(group, kotlin)

        stateHolder.loadIfNeeded(group)

        // The refreshed list is kept, not reset to the group's original issues.
        assertEquals(3, stateHolder.state.value.issues.size)
    }

    @Test
    fun refreshingFetchesTheReposIssuesAndHoldsThem() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1), issueDto(2), issueDto(3))) }
        val stateHolder = opened(f)
        val group = repoGroupOf("octo", "repo", 1)
        stateHolder.loadIfNeeded(group)

        stateHolder.refreshThisRepo(group, kotlin)

        val state = stateHolder.state.value
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
        val stateHolder = opened(f)

        stateHolder.refreshThisRepo(group, kotlin)

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
        val stateHolder = opened(f)

        stateHolder.refreshThisRepo(group, kotlin)

        assertEquals(listOf("other/thing"), f.cacheStorage.data.getValue(cacheKeyFor(kotlin, gfi)).repoGroups.map { it.repoFullName })
        assertTrue(stateHolder.state.value.issues.isEmpty())
    }

    @Test
    fun anErrorResponseKeepsItsCodeAndLeavesTheIssuesAlone() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> searchFailed(500) }
        val stateHolder = opened(f)
        val group = repoGroupOf("octo", "repo", 1, 2)
        stateHolder.loadIfNeeded(group)

        stateHolder.refreshThisRepo(group, kotlin)

        val state = stateHolder.state.value
        assertEquals(SearchFailure.Http(500), state.refreshFailure)
        assertFalse(state.isRefreshing)
        assertEquals(group.issues, state.issues)
    }

    @Test
    fun aFailedRequestKeepsWhatWentWrong() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> throw java.io.IOException("no network") }
        val stateHolder = opened(f)

        stateHolder.refreshThisRepo(repoGroupOf("octo", "repo", 1), kotlin)

        assertEquals(SearchFailure.Failed("no network"), stateHolder.state.value.refreshFailure)
        assertFalse(stateHolder.state.value.isRefreshing)
    }

    @Test
    fun withoutATokenRefreshingDoesNothing() {
        val f = ViewModelFixtures(buildToken = "")
        val stateHolder = opened(f)

        stateHolder.refreshThisRepo(repoGroupOf("octo", "repo", 1), kotlin)

        assertTrue(f.api.calls.isEmpty())
        assertFalse(stateHolder.state.value.isRefreshing)
    }

    @Test
    fun openingAgainStartsAFreshVisitWithNothingLeftOver() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1), issueDto(2))) }
        val stateHolder = opened(f)
        val group = repoGroupOf("octo", "repo", 1)
        stateHolder.loadIfNeeded(group)
        stateHolder.refreshThisRepo(group, kotlin)
        assertTrue(stateHolder.state.value.hasRefreshed)

        stateHolder.open()

        assertEquals(RepoIssuesState(), stateHolder.state.value)
        // The same repo can be loaded again after a fresh visit.
        stateHolder.loadIfNeeded(group)
        assertEquals(group.issues, stateHolder.state.value.issues)
    }
}
