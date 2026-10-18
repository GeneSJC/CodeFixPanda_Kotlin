package com.example.githubissuefinder.ui

import com.example.githubissuefinder.data.AppConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** BaseViewModel over a config: each getter forwards the matching AppConfig value. */
class BaseViewModelTest {

    private class TestViewModel(config: AppConfig) : BaseViewModel(config)

    private fun viewModel(config: AppConfig) = TestViewModel(config)

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
}
