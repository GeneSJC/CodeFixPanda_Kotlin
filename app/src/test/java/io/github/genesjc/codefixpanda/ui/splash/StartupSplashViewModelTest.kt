package io.github.genesjc.codefixpanda.ui.splash

import io.github.genesjc.codefixpanda.data.StartupSplashRepository
import io.github.genesjc.codefixpanda.data.StartupSplashStorage
import io.github.genesjc.codefixpanda.testutil.StopKoinRule
import io.github.genesjc.codefixpanda.testutil.restartKoin
import io.github.genesjc.codefixpanda.testutil.testAppConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StartupSplashViewModelTest {

    @get:Rule
    val stopKoin = StopKoinRule()

    private fun createViewModel(repository: StartupSplashRepository): StartupSplashViewModel {
        restartKoin {
            single { repository }
            single { testAppConfig() }
        }

        return StartupSplashViewModel()
    }

    private class FakeStorage(var seen: Boolean = false) : StartupSplashStorage {
        override fun hasSeenFullAnimation(): Boolean = seen
        override fun markFullAnimationSeen() {
            seen = true
        }
    }

    @Test
    fun playsTheFullAnimationOnTheFirstLaunch() {
        val viewModel = createViewModel(StartupSplashRepository(FakeStorage()))
        assertTrue(viewModel.playFullAnimation)
    }

    @Test
    fun playsTheShortSplashOnceTheFullOneWasSeen() {
        val viewModel = createViewModel(StartupSplashRepository(FakeStorage(seen = true)))
        assertFalse(viewModel.playFullAnimation)
    }

    @Test
    fun finishingRecordsThatTheAnimationWasSeen() {
        val storage = FakeStorage()
        createViewModel(StartupSplashRepository(storage)).onSplashFinished()
        assertTrue(storage.seen)
    }

    /** The decision is made once per launch: finishing must not flip a splash that is still on screen. */
    @Test
    fun theDecisionDoesNotFlipAfterFinishing() {
        val viewModel = createViewModel(StartupSplashRepository(FakeStorage()))
        viewModel.onSplashFinished()
        assertTrue(viewModel.playFullAnimation)
    }

    @Test
    fun theNextLaunchGetsTheShortSplash() {
        val storage = FakeStorage()
        createViewModel(StartupSplashRepository(storage)).onSplashFinished()

        val nextLaunch = createViewModel(StartupSplashRepository(storage))

        assertFalse(nextLaunch.playFullAnimation)
    }
}
