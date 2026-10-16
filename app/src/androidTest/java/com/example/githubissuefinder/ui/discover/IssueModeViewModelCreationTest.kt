package com.example.githubissuefinder.ui.discover

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.githubissuefinder.data.ApiTokenRepository
import com.example.githubissuefinder.data.BookmarksRepository
import com.example.githubissuefinder.data.HiddenIssuesRepository
import com.example.githubissuefinder.data.IssueModeRepository
import com.example.githubissuefinder.data.StartupSplashRepository
import com.example.githubissuefinder.ui.settings.ApiTokenViewModel
import com.example.githubissuefinder.ui.splash.StartupSplashViewModel
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Creates each Issue Mode ViewModel through the real Android
 * ViewModelProvider machinery — the same reflection-based factory
 * Compose's viewModel() uses — which JVM unit tests can't do because
 * TopReposViewModel needs an Application. This guards the riskiest part
 * of the MVVM refactor: constructors with default arguments, plus the
 * JvmOverloads annotation on TopReposViewModel's, must still be found
 * by that factory, or the app would crash the moment a screen asks for
 * its ViewModel.
 *
 * Runs on a device or emulator. Creating TopReposViewModel starts its
 * initial load, which makes a real network request if a token is
 * configured; the test only checks construction, and clearing the store
 * in tearDown cancels that work.
 */
@RunWith(AndroidJUnit4::class)
class IssueModeViewModelCreationTest {

    private lateinit var application: Application
    private lateinit var store: ViewModelStore

    @Before
    fun setUp() {
        application = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as Application
        IssueModeRepository.init(application)
        StartupSplashRepository.init(application)
        ApiTokenRepository.init(application)
        HiddenIssuesRepository.init(application)
        BookmarksRepository.init(application)
        store = ViewModelStore()
    }

    @After
    fun tearDown() {
        store.clear()
    }

    private fun provider() = ViewModelProvider(
        store,
        ViewModelProvider.AndroidViewModelFactory.getInstance(application),
    )

    @Test
    fun issueModeViewModelIsCreatedByTheDefaultFactory() {
        assertNotNull(provider()[IssueModeViewModel::class.java])
    }

    @Test
    fun repoIssuesViewModelIsCreatedByTheDefaultFactory() {
        assertNotNull(provider()[RepoIssuesViewModel::class.java])
    }

    @Test
    fun topReposViewModelIsCreatedByTheDefaultFactory() {
        assertNotNull(provider()[TopReposViewModel::class.java])
    }

    @Test
    fun startupSplashViewModelIsCreatedByTheDefaultFactory() {
        assertNotNull(provider()[StartupSplashViewModel::class.java])
    }

    @Test
    fun apiTokenViewModelIsCreatedByTheDefaultFactory() {
        assertNotNull(provider()[ApiTokenViewModel::class.java])
    }

    @Test
    fun hiddenIssuesViewModelIsCreatedByTheDefaultFactory() {
        assertNotNull(provider()[HiddenIssuesViewModel::class.java])
    }

    @Test
    fun bookmarksViewModelIsCreatedByTheDefaultFactory() {
        assertNotNull(provider()[BookmarksViewModel::class.java])
    }
}
