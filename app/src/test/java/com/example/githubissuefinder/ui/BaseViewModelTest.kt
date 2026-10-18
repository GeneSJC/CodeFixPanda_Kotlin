package com.example.githubissuefinder.ui

import com.example.githubissuefinder.data.AppConfig
import com.example.githubissuefinder.data.AppConfigRepository
import com.example.githubissuefinder.data.BookmarksRepository
import com.example.githubissuefinder.data.HiddenIssuesRepository
import com.example.githubissuefinder.testutil.ViewModelFixtures
import com.example.githubissuefinder.testutil.repoGroupOf
import com.example.githubissuefinder.testutil.testBookmarksRepository
import com.example.githubissuefinder.testutil.testHiddenIssuesRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** BaseViewModel over a fake config: each getter forwards the matching AppConfig value. */
class BaseViewModelTest {

    private class TestViewModel(
        appConfigRepository: AppConfigRepository,
        hiddenIssuesRepository: HiddenIssuesRepository,
        bookmarksRepository: BookmarksRepository,
    ) : BaseViewModel(appConfigRepository, hiddenIssuesRepository, bookmarksRepository)

    private fun viewModel(config: AppConfig) =
        TestViewModel(AppConfigRepository(config), testHiddenIssuesRepository(), testBookmarksRepository())

    private fun viewModelOver(f: ViewModelFixtures) =
        TestViewModel(f.appConfigRepository, f.hiddenIssuesRepository, f.bookmarksRepository)

    @Test
    fun gettersForwardTheConfigValues() {
        val vm = viewModel(AppConfig(buildLabel = "24.1", splashAnimation = "origami", showDetailedDrawerMenu = true))

        assertEquals("24.1", vm.buildLabel)
        assertEquals("origami", vm.splashAnimation)
        assertTrue(vm.showDetailedDrawerMenu)
    }

    @Test
    fun theDrawerFlagCanBeOff() {
        val vm = viewModel(AppConfig(buildLabel = "1.0", splashAnimation = "coin", showDetailedDrawerMenu = false))

        assertFalse(vm.showDetailedDrawerMenu)
    }

    @Test
    fun hiddenIdsComeFromTheHiddenIssuesRepository() {
        val f = ViewModelFixtures()
        val vm = viewModelOver(f)
        assertTrue(vm.hiddenIds.value.isEmpty())

        f.hiddenIssuesRepository.hide(repoGroupOf("octo", "repo", 1).issues.single())

        assertEquals(setOf(1L), vm.hiddenIds.value)
    }

    @Test
    fun bookmarkedIdsFollowToggleBookmark() {
        val f = ViewModelFixtures()
        val vm = viewModelOver(f)
        val issue = repoGroupOf("octo", "repo", 3).issues.single()

        assertTrue(vm.toggleBookmark(issue))
        assertEquals(setOf(3L), vm.bookmarkedIds.value)

        assertFalse(vm.toggleBookmark(issue))
        assertTrue(vm.bookmarkedIds.value.isEmpty())
    }
}
