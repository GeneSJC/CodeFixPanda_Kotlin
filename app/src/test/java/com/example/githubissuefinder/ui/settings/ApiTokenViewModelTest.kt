package com.example.githubissuefinder.ui.settings

import com.example.githubissuefinder.data.ApiTokenRepository
import com.example.githubissuefinder.data.ApiTokenStorage
import com.example.githubissuefinder.data.TokenSource
import com.example.githubissuefinder.testutil.StopKoinRule
import com.example.githubissuefinder.testutil.restartKoin
import com.example.githubissuefinder.testutil.testAppConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ApiTokenViewModelTest {

    @get:Rule
    val stopKoin = StopKoinRule()

    private class FakeStorage(var stored: String? = null) : ApiTokenStorage {
        override fun load(): String? = stored
        override fun save(token: String?) {
            stored = token
        }
    }

    private fun viewModel(storage: FakeStorage = FakeStorage(), buildToken: String = ""): ApiTokenViewModel {
        val repository = ApiTokenRepository(storage, buildToken)
        restartKoin {
            single { repository }
            single { testAppConfig() }
        }
        return ApiTokenViewModel()
    }

    @Test
    fun startsEmptyWithNoSourceWhenNothingIsConfigured() {
        val vm = viewModel()
        assertEquals("", vm.userToken.value)
        assertEquals(TokenSource.NONE, vm.tokenSource.value)
    }

    @Test
    fun reportsTheBuildTokenAsTheSourceWhenOnlyThatExists() {
        assertEquals(TokenSource.BUILD, viewModel(buildToken = "build").tokenSource.value)
    }

    @Test
    fun saveTokenUpdatesWhatIsObservedAndPersists() {
        val storage = FakeStorage()
        val vm = viewModel(storage)

        vm.saveToken("mine")

        assertEquals("mine", vm.userToken.value)
        assertEquals(TokenSource.USER, vm.tokenSource.value)
        assertEquals("mine", storage.stored)
    }

    @Test
    fun clearTokenRemovesItAndFallsBackToTheBuildToken() {
        val storage = FakeStorage()
        val vm = viewModel(storage, buildToken = "build")
        vm.saveToken("mine")

        vm.clearToken()

        assertEquals("", vm.userToken.value)
        assertEquals(TokenSource.BUILD, vm.tokenSource.value)
        assertNull(storage.stored)
    }
}
