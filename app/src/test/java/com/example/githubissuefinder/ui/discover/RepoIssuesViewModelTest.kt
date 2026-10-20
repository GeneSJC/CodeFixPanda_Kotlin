package com.example.githubissuefinder.ui.discover

import com.example.githubissuefinder.testutil.FakeStringResolver
import com.example.githubissuefinder.R
import com.example.githubissuefinder.model.IssueLabelFilter
import com.example.githubissuefinder.model.LanguageFilter
import com.example.githubissuefinder.testutil.MainDispatcherRule
import com.example.githubissuefinder.testutil.StopKoinRule
import com.example.githubissuefinder.testutil.ViewModelFixtures
import com.example.githubissuefinder.testutil.issueDto
import com.example.githubissuefinder.testutil.repoGroupOf
import com.example.githubissuefinder.testutil.searchFailed
import com.example.githubissuefinder.testutil.searchOk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * RepoIssuesViewModel over the real RepoIssuesStateHolder (built on
 * fakes). What the state holder
 * decides is covered in RepoIssuesStateHolderTest; this covers what the
 * ViewModel adds: the message text, the forwarding of actions, and the
 * hide and bookmark pass-throughs.
 */
class RepoIssuesViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    @get:Rule
    val stopKoin = StopKoinRule()

    private val s = FakeStringResolver
    private val kotlin = LanguageFilter.KOTLIN

    private fun viewModel(f: ViewModelFixtures): RepoIssuesViewModel = f.repoIssuesViewModel()

    @Test
    fun theDemoBannerIsOnExactlyWhileThereIsNoToken() {
        assertTrue(viewModel(ViewModelFixtures(buildToken = "")).isDemo.value)
        assertFalse(viewModel(ViewModelFixtures()).isDemo.value)
    }

    @Test
    fun loadingAGroupShowsItsIssues() {
        val vm = viewModel(ViewModelFixtures())
        val group = repoGroupOf("octo", "repo", 1, 2)

        vm.loadIfNeeded(group)

        assertEquals(group.issues, vm.uiState.value.issues)
    }

    @Test
    fun refreshingShowsTheCompleteListAndTheResultFlags() {
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
    }

    @Test
    fun anErrorResponseShowsItsCodeAndLeavesTheIssuesAlone() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> searchFailed(500) }
        val vm = viewModel(f)
        val group = repoGroupOf("octo", "repo", 1, 2)
        vm.loadIfNeeded(group)

        vm.refreshThisRepo(group, kotlin)

        assertEquals(s(R.string.error_http, 500), vm.uiState.value.refreshError)
        assertEquals(group.issues, vm.uiState.value.issues)
    }

    @Test
    fun aFailedRequestShowsWhatWentWrong() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> throw java.io.IOException("no network") }
        val vm = viewModel(f)

        vm.refreshThisRepo(repoGroupOf("octo", "repo", 1), kotlin)

        assertEquals(s(R.string.refresh_repo_failed, "no network"), vm.uiState.value.refreshError)
    }

    @Test
    fun theModeComesFromTheStateHolder() {
        val vm = viewModel(ViewModelFixtures(mode = IssueLabelFilter.HELP_WANTED))

        assertEquals(IssueLabelFilter.HELP_WANTED, vm.mode.value)
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
