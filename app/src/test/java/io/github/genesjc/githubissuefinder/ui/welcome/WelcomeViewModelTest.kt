package io.github.genesjc.githubissuefinder.ui.welcome

import io.github.genesjc.githubissuefinder.data.ApiTokenRepository
import io.github.genesjc.githubissuefinder.data.WelcomeRepository
import io.github.genesjc.githubissuefinder.data.WelcomeStorage
import io.github.genesjc.githubissuefinder.testutil.FakeApiTokenStorage
import io.github.genesjc.githubissuefinder.testutil.StopKoinRule
import io.github.genesjc.githubissuefinder.testutil.restartKoin
import io.github.genesjc.githubissuefinder.testutil.testAppConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WelcomeViewModelTest {

    @get:Rule
    val stopKoin = StopKoinRule()

    private class FakeWelcomeStorage(var seen: Boolean = false) : WelcomeStorage {
        override fun hasSeenWelcome(): Boolean = seen
        override fun markWelcomeSeen() {
            seen = true
        }
    }

    private val tokenStorage = FakeApiTokenStorage()
    private val welcomeStorage = FakeWelcomeStorage()
    private val tokens = ApiTokenRepository(tokenStorage, buildToken = "")

    private fun viewModel(): WelcomeViewModel {
        restartKoin {
            single { tokens }
            single { WelcomeRepository(welcomeStorage) }
            single { testAppConfig() }
        }
        return WelcomeViewModel()
    }

    @Test
    fun theWelcomeIsNeededAtStartOnlyWithNoTokenAndNotYetSeen() {
        assertTrue(viewModel().needsWelcomeAtStart())

        welcomeStorage.seen = true
        assertFalse(viewModel().needsWelcomeAtStart())
    }

    @Test
    fun aSavedTokenMeansTheWelcomeIsNotNeededAtStart() {
        tokens.setUserToken("ghp_abc")

        assertFalse(viewModel().needsWelcomeAtStart())
    }

    @Test
    fun savingATokenStoresItAndMarksTheWelcomeSeen() {
        val vm = viewModel()

        assertTrue(vm.saveToken("  ghp_abc  "))

        assertEquals("ghp_abc", tokens.userToken.value)
        assertEquals("ghp_abc", tokenStorage.stored)
        assertTrue(welcomeStorage.seen)
        assertFalse(tokens.isDemo.value)
    }

    @Test
    fun aBlankTokenSavesNothingAndLeavesTheWelcomeOpen() {
        val vm = viewModel()

        assertFalse(vm.saveToken("   "))

        assertEquals("", tokens.userToken.value)
        assertFalse(welcomeStorage.seen)
    }

    @Test
    fun tryingTheDemoMarksTheWelcomeSeenWithoutAToken() {
        val vm = viewModel()

        vm.finish()

        assertTrue(welcomeStorage.seen)
        assertEquals("", tokens.userToken.value)
        assertTrue(tokens.isDemo.value)
    }
}
