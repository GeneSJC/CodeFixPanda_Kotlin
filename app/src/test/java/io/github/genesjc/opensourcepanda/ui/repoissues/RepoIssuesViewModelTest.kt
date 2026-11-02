package io.github.genesjc.opensourcepanda.ui.repoissues

import io.github.genesjc.opensourcepanda.R
import io.github.genesjc.opensourcepanda.model.IssueLabelFilter
import io.github.genesjc.opensourcepanda.model.LanguageFilter
import io.github.genesjc.opensourcepanda.testutil.FakeStringResolver
import io.github.genesjc.opensourcepanda.testutil.MainDispatcherRule
import io.github.genesjc.opensourcepanda.testutil.StopKoinRule
import io.github.genesjc.opensourcepanda.testutil.ViewModelFixtures
import io.github.genesjc.opensourcepanda.testutil.issueDto
import io.github.genesjc.opensourcepanda.testutil.repoGroupOf
import io.github.genesjc.opensourcepanda.testutil.searchFailed
import io.github.genesjc.opensourcepanda.testutil.searchOk
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
    fun canShowAdsFollowsTheAdsController() {
        val f = ViewModelFixtures()
        val vm = viewModel(f)
        assertFalse(vm.canShowAds.value)

        f.adsController.canShowAds.value = true

        assertTrue(vm.canShowAds.value)
    }

    @Test
    fun adFreeStateFollowsTheAdsController() {
        val f = ViewModelFixtures()
        val vm = viewModel(f)
        assertEquals(0L, vm.adFreeUntilMs.value)
        assertFalse(vm.canOfferAdFree.value)

        f.adsController.adFreeUntilMs.value = 123L
        f.adsController.canOfferAdFree.value = true

        assertEquals(123L, vm.adFreeUntilMs.value)
        assertTrue(vm.canOfferAdFree.value)
    }

    @Test
    fun theDemoMarkerIsOnExactlyWhileThereIsNoToken() {
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

        assertEquals(IssueLabelFilter.HELP_WANTED, vm.activeMode.value)
    }

    @Test
    fun theActiveModeKeepsTheLastChosenModeWhenTheModeIsCleared() {
        val f = ViewModelFixtures(mode = IssueLabelFilter.HELP_WANTED)
        val vm = viewModel(f)

        f.modeRepository.setMode(null)

        assertEquals(IssueLabelFilter.HELP_WANTED, vm.activeMode.value)
    }

    @Test
    fun hidingAndUnhidingGoThroughTheRepository() {
        val f = ViewModelFixtures()
        val vm = viewModel(f)
        val group = repoGroupOf("octo", "repo", 1, 2)
        vm.loadIfNeeded(group)

        vm.hideIssue(group.issues.first())
        assertEquals(listOf(2L), vm.visibleIssues.value.map { it.id })
        assertEquals(group.issues, vm.uiState.value.issues)

        vm.unhideIssue(1L)
        assertEquals(listOf(1L, 2L), vm.visibleIssues.value.map { it.id })
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
