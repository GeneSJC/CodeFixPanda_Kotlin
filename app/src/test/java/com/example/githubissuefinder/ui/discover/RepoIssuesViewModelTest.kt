package com.example.githubissuefinder.ui.discover

import com.example.githubissuefinder.data.REPO_ISSUES_MAX_PER_PAGE
import com.example.githubissuefinder.data.RepoIssueGroupCacheEntry
import com.example.githubissuefinder.data.cacheKeyFor
import com.example.githubissuefinder.data.repoIssuesQuery
import com.example.githubissuefinder.model.IssueLabelFilter
import com.example.githubissuefinder.model.LanguageFilter
import com.example.githubissuefinder.testutil.MainDispatcherRule
import com.example.githubissuefinder.testutil.ViewModelFixtures
import com.example.githubissuefinder.testutil.issueDto
import com.example.githubissuefinder.testutil.repoGroupOf
import com.example.githubissuefinder.testutil.searchFailed
import com.example.githubissuefinder.testutil.searchOk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** RepoIssuesViewModel against fakes — see TopReposViewModelTest for the approach. */
class RepoIssuesViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val gfi = IssueLabelFilter.GOOD_FIRST_ISSUE
    private val kotlin = LanguageFilter.KOTLIN

    @After
    fun tearDown() {
        ViewModelFixtures.uninstall()
    }

    private fun viewModel(f: ViewModelFixtures): RepoIssuesViewModel {
        f.installRepositories()
        return RepoIssuesViewModel(f.modeRepository, f.cacheRepository, f.searchRepository, f.tokenRepository)
    }

    private fun cacheWith(f: ViewModelFixtures, vararg groups: com.example.githubissuefinder.model.RepoIssueGroup) {
        f.cacheStorage.data = mapOf(
            cacheKeyFor(kotlin, gfi) to RepoIssueGroupCacheEntry(groups.toList(), 5, 2, true, 777L),
        )
    }

    @Test
    fun loadingAGroupShowsItsIssues() {
        val vm = viewModel(ViewModelFixtures())
        val group = repoGroupOf("octo", "repo", 1, 2)

        vm.loadIfNeeded(group)

        assertEquals(group.issues, vm.uiState.value.issues)
    }

    @Test
    fun refreshingFetchesTheReposIssuesAndShowsThem() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1), issueDto(2), issueDto(3))) }
        val vm = viewModel(f)
        val group = repoGroupOf("octo", "repo", 1)
        vm.loadIfNeeded(group)

        vm.refreshThisRepo(group, kotlin)

        val state = vm.uiState.value
        assertEquals(listOf(1L, 2L, 3L), state.issues.map { it.id })
        assertTrue(state.hasRefreshed)
        assertFalse(state.isRefreshing)
        assertNull(state.refreshError)
        assertEquals(Triple(repoIssuesQuery("octo/repo", gfi), REPO_ISSUES_MAX_PER_PAGE, 1), f.api.calls.single())
    }

    @Test
    fun refreshingWritesTheUpdatedGroupBackIntoTheCache() {
        val f = ViewModelFixtures()
        val group = repoGroupOf("octo", "repo", 1)
        cacheWith(f, group, repoGroupOf("other", "thing", 9))
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1), issueDto(2), issueDto(3))) }
        val vm = viewModel(f)

        vm.refreshThisRepo(group, kotlin)

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
        val vm = viewModel(f)

        vm.refreshThisRepo(group, kotlin)

        assertEquals(listOf("other/thing"), f.cacheStorage.data.getValue(cacheKeyFor(kotlin, gfi)).repoGroups.map { it.repoFullName })
        assertTrue(vm.uiState.value.issues.isEmpty())
    }

    @Test
    fun anErrorResponseShowsItsCodeAndLeavesTheIssuesAlone() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> searchFailed(500) }
        val vm = viewModel(f)
        val group = repoGroupOf("octo", "repo", 1, 2)
        vm.loadIfNeeded(group)

        vm.refreshThisRepo(group, kotlin)

        val state = vm.uiState.value
        assertEquals("GitHub returned an error (HTTP 500)", state.refreshError)
        assertFalse(state.isRefreshing)
        assertEquals(group.issues, state.issues)
    }

    @Test
    fun aFailedRequestShowsWhatWentWrong() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> throw java.io.IOException("no network") }
        val vm = viewModel(f)

        vm.refreshThisRepo(repoGroupOf("octo", "repo", 1), kotlin)

        assertEquals("Couldn't refresh this repo: no network", vm.uiState.value.refreshError)
        assertFalse(vm.uiState.value.isRefreshing)
    }

    @Test
    fun withoutATokenRefreshingDoesNothing() {
        val f = ViewModelFixtures(buildToken = "")
        val vm = viewModel(f)

        vm.refreshThisRepo(repoGroupOf("octo", "repo", 1), kotlin)

        assertTrue(f.api.calls.isEmpty())
        assertFalse(vm.uiState.value.isRefreshing)
    }

    @Test
    fun hidingAndUnhidingGoThroughTheRepository() {
        val f = ViewModelFixtures()
        val vm = viewModel(f)
        val issue = repoGroupOf("octo", "repo", 1).issues.single()

        vm.hideIssue(issue)
        assertEquals(setOf(1L), vm.hiddenIds.value)

        vm.unhideIssue(1L)
        assertTrue(vm.hiddenIds.value.isEmpty())
    }

    @Test
    fun togglingABookmarkReportsTheNewStateAndUpdatesTheIds() {
        val f = ViewModelFixtures()
        val vm = viewModel(f)
        val issue = repoGroupOf("octo", "repo", 1).issues.single()

        assertTrue(vm.toggleBookmark(issue))
        assertEquals(setOf(1L), vm.bookmarkedIds.value)

        assertFalse(vm.toggleBookmark(issue))
        assertTrue(vm.bookmarkedIds.value.isEmpty())
    }
}
