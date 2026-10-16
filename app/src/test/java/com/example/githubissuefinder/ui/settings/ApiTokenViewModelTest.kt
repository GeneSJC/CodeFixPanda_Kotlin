package com.example.githubissuefinder.ui.settings

import com.example.githubissuefinder.data.ApiTokenRepository
import com.example.githubissuefinder.data.ApiTokenStorage
import com.example.githubissuefinder.data.TokenSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ApiTokenViewModelTest {

    private class FakeStorage(var stored: String? = null) : ApiTokenStorage {
        override fun load(): String? = stored
        override fun save(token: String?) {
            stored = token
        }
    }

    private fun viewModel(storage: FakeStorage = FakeStorage(), buildToken: String = "") =
        ApiTokenViewModel(ApiTokenRepository(storage, buildToken))

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
