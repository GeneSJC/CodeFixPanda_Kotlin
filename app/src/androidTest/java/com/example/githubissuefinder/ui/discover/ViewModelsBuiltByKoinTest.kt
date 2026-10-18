package com.example.githubissuefinder.ui.discover

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.githubissuefinder.ui.settings.ApiTokenViewModel
import com.example.githubissuefinder.ui.splash.StartupSplashViewModel
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/**
 * Asks the running app's Koin container for each ViewModel. The test
 * process runs the app's own Application class, so Koin has already been
 * started with the real module; if any ViewModel's constructor asks for
 * something the module does not define, or the Android parts (a Context,
 * the preferences and files behind the storages) cannot be built, this
 * fails here instead of the app crashing the moment a screen opens.
 *
 * Runs on a device or emulator. Building TopReposViewModel starts its
 * initial load, which makes a real network request if a token is
 * configured; the test only checks construction.
 */
@RunWith(AndroidJUnit4::class)
class ViewModelsBuiltByKoinTest {

    private val koin get() = GlobalContext.get()

    @Test
    fun issueModeViewModelIsBuilt() {
        assertNotNull(koin.get<IssueModeViewModel>())
    }

    @Test
    fun repoIssuesViewModelIsBuilt() {
        assertNotNull(koin.get<RepoIssuesViewModel>())
    }

    @Test
    fun topReposViewModelIsBuilt() {
        assertNotNull(koin.get<TopReposViewModel>())
    }

    @Test
    fun startupSplashViewModelIsBuilt() {
        assertNotNull(koin.get<StartupSplashViewModel>())
    }

    @Test
    fun apiTokenViewModelIsBuilt() {
        assertNotNull(koin.get<ApiTokenViewModel>())
    }

    @Test
    fun hiddenIssuesViewModelIsBuilt() {
        assertNotNull(koin.get<HiddenIssuesViewModel>())
    }

    @Test
    fun bookmarksViewModelIsBuilt() {
        assertNotNull(koin.get<BookmarksViewModel>())
    }

    @Test
    fun repoReadmeViewModelIsBuilt() {
        assertNotNull(koin.get<RepoReadmeViewModel>())
    }
}
