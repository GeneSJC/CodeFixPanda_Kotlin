package com.example.githubissuefinder.ui

import com.example.githubissuefinder.data.AppConfig
import com.example.githubissuefinder.data.AppConfigRepository
import com.example.githubissuefinder.testutil.ViewModelFixtures
import com.example.githubissuefinder.testutil.repoGroupOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** BaseViewModel over a fake config: each getter forwards the matching AppConfig value. */
class BaseViewModelTest {

    private class TestViewModel(repository: AppConfigRepository) : BaseViewModel(repository)

    private fun viewModel(config: AppConfig) = TestViewModel(AppConfigRepository(config))

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

    @After
    fun tearDown() {
        ViewModelFixtures.uninstall()
    }

    @Test
    fun hiddenIdsComeFromTheHiddenIssuesRepository() {
        val f = ViewModelFixtures()
        f.installRepositories()
        val vm = viewModel(AppConfig(buildLabel = "1.0", splashAnimation = "coin", showDetailedDrawerMenu = false))
        assertTrue(vm.hiddenIds.value.isEmpty())

        f.hiddenIssuesRepository.hide(repoGroupOf("octo", "repo", 1).issues.single())

        assertEquals(setOf(1L), vm.hiddenIds.value)
    }

    @Test
    fun bookmarkedIdsFollowToggleBookmark() {
        val f = ViewModelFixtures()
        f.installRepositories()
        val vm = viewModel(AppConfig(buildLabel = "1.0", splashAnimation = "coin", showDetailedDrawerMenu = false))
        val issue = repoGroupOf("octo", "repo", 3).issues.single()

        assertTrue(vm.toggleBookmark(issue))
        assertEquals(setOf(3L), vm.bookmarkedIds.value)

        assertFalse(vm.toggleBookmark(issue))
        assertTrue(vm.bookmarkedIds.value.isEmpty())
    }
}
