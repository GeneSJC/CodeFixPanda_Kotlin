package com.example.githubissuefinder.ui

import com.example.githubissuefinder.ui.discover.BookmarksViewModel
import com.example.githubissuefinder.ui.discover.HiddenIssuesViewModel
import com.example.githubissuefinder.ui.discover.IssueModeViewModel
import com.example.githubissuefinder.ui.discover.RepoIssuesViewModel
import com.example.githubissuefinder.ui.discover.RepoReadmeViewModel
import com.example.githubissuefinder.ui.discover.TopReposViewModel
import com.example.githubissuefinder.ui.settings.ApiTokenViewModel
import com.example.githubissuefinder.ui.splash.StartupSplashViewModel
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The project convention: every top-level (screen-facing) ViewModel
 * extends BaseViewModel unless there is a stated reason not to (see
 * Conventions 4 — Architecture Design). This keeps the convention from
 * quietly eroding. A new ViewModel is added to the list below; one that
 * deliberately does not extend the base would be left out with a comment
 * here saying why.
 */
class AllViewModelsExtendBaseTest {

    @Test
    fun everyScreenViewModelExtendsTheBase() {
        val viewModels = listOf(
            TopReposViewModel::class.java,
            RepoIssuesViewModel::class.java,
            RepoReadmeViewModel::class.java,
            BookmarksViewModel::class.java,
            HiddenIssuesViewModel::class.java,
            IssueModeViewModel::class.java,
            ApiTokenViewModel::class.java,
            StartupSplashViewModel::class.java,
        )

        viewModels.forEach { viewModel ->
            assertTrue("${viewModel.simpleName} should extend BaseViewModel", BaseViewModel::class.java.isAssignableFrom(viewModel))
        }
    }
}
