package io.github.genesjc.opensourcepanda.ui.welcome

import io.github.genesjc.opensourcepanda.data.ApiTokenRepository
import io.github.genesjc.opensourcepanda.model.TokenSource
import io.github.genesjc.opensourcepanda.testutil.FakeApiTokenStorage
import io.github.genesjc.opensourcepanda.testutil.StopKoinRule
import io.github.genesjc.opensourcepanda.testutil.restartKoin
import io.github.genesjc.opensourcepanda.testutil.testAppConfig
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
    fun theCurrentTokenIsTheSavedOrBuildTokenOrEmpty() {
        assertEquals("", viewModel().currentToken)
        assertEquals("build-token", viewModel(buildToken = "build-token").currentToken)

        tokenStorage.stored = "ghp_abc"
        assertEquals("ghp_abc", viewModel(buildToken = "build-token").currentToken)
    }

    @Test
    fun clearingRemovesTheSavedTokenFromStorageAndFallsBackToTheBuildToken() {
        tokenStorage.stored = "ghp_abc"
        val vm = viewModel(buildToken = "build-token")
        assertEquals("ghp_abc", vm.userToken.value)

        vm.clearToken()

        assertEquals("", vm.userToken.value)
        assertEquals(null, tokenStorage.stored)
        assertEquals("build-token", vm.currentToken)
    }

    @Test
    fun clearingWithNoBuildTokenLeavesNoToken() {
        tokenStorage.stored = "ghp_abc"
        val vm = viewModel()

        vm.clearToken()

        assertEquals(null, tokenStorage.stored)
        assertTrue(vm.needsWelcomeAtStart())
    }

    @Test
    fun theTokenSourceSaysWhereTheTokenInUseCameFrom() {
        assertEquals(TokenSource.NONE, viewModel().tokenSource.value)
        assertEquals(TokenSource.BUILD, viewModel(buildToken = "build-token").tokenSource.value)

        tokenStorage.stored = "ghp_abc"
        assertEquals(TokenSource.USER, viewModel(buildToken = "build-token").tokenSource.value)
    }

    @Test
    fun tryingTheDemoShowsDemoDataEvenWithABuildToken() {
        val vm = viewModel(buildToken = "build-token")
        assertEquals(TokenSource.BUILD, vm.tokenSource.value)

        vm.tryDemo()

        assertEquals(TokenSource.NONE, vm.tokenSource.value)
        assertEquals("", vm.currentToken)
    }

    @Test
    fun aBlankTokenSavesNothing() {
        val vm = viewModel()

        assertFalse(vm.saveToken("   "))

        assertEquals(null, tokenStorage.stored)
        assertTrue(vm.needsWelcomeAtStart())
    }
}
