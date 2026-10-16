package com.example.githubissuefinder.ui.splash

import com.example.githubissuefinder.data.StartupSplashRepository
import com.example.githubissuefinder.data.StartupSplashStorage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupSplashViewModelTest {

    private class FakeStorage(var seen: Boolean = false) : StartupSplashStorage {
        override fun hasSeenFullAnimation(): Boolean = seen
        override fun markFullAnimationSeen() {
            seen = true
        }
    }

    @Test
    fun playsTheFullAnimationOnTheFirstLaunch() {
        val viewModel = StartupSplashViewModel(StartupSplashRepository(FakeStorage()))
        assertTrue(viewModel.playFullAnimation)
    }

    @Test
    fun playsTheShortSplashOnceTheFullOneWasSeen() {
        val viewModel = StartupSplashViewModel(StartupSplashRepository(FakeStorage(seen = true)))
        assertFalse(viewModel.playFullAnimation)
    }

    @Test
    fun finishingRecordsThatTheAnimationWasSeen() {
        val storage = FakeStorage()
        StartupSplashViewModel(StartupSplashRepository(storage)).onSplashFinished()
        assertTrue(storage.seen)
    }

    /** The decision is made once per launch: finishing must not flip a splash that is still on screen. */
    @Test
    fun theDecisionDoesNotFlipAfterFinishing() {
        val viewModel = StartupSplashViewModel(StartupSplashRepository(FakeStorage()))
        viewModel.onSplashFinished()
        assertTrue(viewModel.playFullAnimation)
    }

    @Test
    fun theNextLaunchGetsTheShortSplash() {
        val storage = FakeStorage()
        StartupSplashViewModel(StartupSplashRepository(storage)).onSplashFinished()

        val nextLaunch = StartupSplashViewModel(StartupSplashRepository(storage))

        assertFalse(nextLaunch.playFullAnimation)
    }
}
