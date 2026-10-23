package io.github.genesjc.githubissuefinder.ui

import io.github.genesjc.githubissuefinder.ui.ads.AdsViewModel
import io.github.genesjc.githubissuefinder.ui.discover.BookmarksViewModel
import io.github.genesjc.githubissuefinder.ui.discover.HiddenIssuesViewModel
import io.github.genesjc.githubissuefinder.ui.discover.IssueModeViewModel
import io.github.genesjc.githubissuefinder.ui.discover.RepoIssuesViewModel
import io.github.genesjc.githubissuefinder.ui.discover.RepoReadmeViewModel
import io.github.genesjc.githubissuefinder.ui.discover.TopReposViewModel
import io.github.genesjc.githubissuefinder.ui.settings.ApiTokenViewModel
import io.github.genesjc.githubissuefinder.ui.splash.StartupSplashViewModel
import io.github.genesjc.githubissuefinder.ui.welcome.WelcomeViewModel
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
            AdsViewModel::class.java,
            WelcomeViewModel::class.java,
        )

        viewModels.forEach { viewModel ->
            assertTrue("${viewModel.simpleName} should extend BaseViewModel", BaseViewModel::class.java.isAssignableFrom(viewModel))
        }
    }
}
