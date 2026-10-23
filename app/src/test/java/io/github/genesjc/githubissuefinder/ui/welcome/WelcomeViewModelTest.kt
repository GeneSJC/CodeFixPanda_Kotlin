package io.github.genesjc.githubissuefinder.ui.welcome

import io.github.genesjc.githubissuefinder.data.ApiTokenRepository
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

    private val tokenStorage = FakeApiTokenStorage()

    private fun viewModel(buildToken: String = ""): WelcomeViewModel {
        val tokens = ApiTokenRepository(tokenStorage, buildToken)
        restartKoin {
            single { tokens }
            single { testAppConfig() }
        }
        return WelcomeViewModel()
    }

    @Test
    fun theWelcomeIsNeededAtStartWhileThereIsNoToken() {
        assertTrue(viewModel().needsWelcomeAtStart())
    }

    @Test
    fun aSavedTokenMeansTheWelcomeIsNotNeededAtStart() {
        tokenStorage.stored = "ghp_abc"

        assertFalse(viewModel().needsWelcomeAtStart())
    }

    @Test
    fun aBuildTokenMeansTheWelcomeIsNotNeededAtStart() {
        assertFalse(viewModel(buildToken = "build-token").needsWelcomeAtStart())
    }

    @Test
    fun savingATokenStoresItAndEndsTheNeedForTheWelcome() {
        val vm = viewModel()

        assertTrue(vm.saveToken("  ghp_abc  "))

        assertEquals("ghp_abc", tokenStorage.stored)
        assertFalse(vm.needsWelcomeAtStart())
    }

    @Test
    fun aBlankTokenSavesNothing() {
        val vm = viewModel()

        assertFalse(vm.saveToken("   "))

        assertEquals(null, tokenStorage.stored)
        assertTrue(vm.needsWelcomeAtStart())
    }
}
