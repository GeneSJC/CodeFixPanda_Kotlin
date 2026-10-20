package io.github.genesjc.githubissuefinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.genesjc.githubissuefinder.ui.discover.DiscoverNavHost
import io.github.genesjc.githubissuefinder.ui.discover.IssueModeViewModel
import io.github.genesjc.githubissuefinder.ui.splash.AppStartSplashScreen
import io.github.genesjc.githubissuefinder.ui.splash.StartupSplashViewModel
import io.github.genesjc.githubissuefinder.ui.theme.GitHubIssueFinderTheme

/**
 * The manifest's android:configChanges stops the system from recreating this Activity on rotation.
 * Recreation reset `showSplash` (replaying the start animation on a running app) and was the
 * suspected cause of a white screen after rotating to landscape. With it, Compose just recomposes
 * in place, which is how TopReposScreen's landscape detection expects to work.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // App-wide Issue Mode (v0.41): observed through
            // IssueModeViewModel at this single root theme call, which
            // wraps the whole app — splash and the Discover flow alike.
            // viewModel() here is Activity-scoped, so DiscoverNavHost's
            // own viewModel() call gets this same instance and the
            // theme updates the moment the gate screen's choice lands.
            // Null (no mode chosen yet) falls back to the original
            // light/dark scheme unchanged.
            val issueModeViewModel: IssueModeViewModel = viewModel()
            val issueMode by issueModeViewModel.mode.collectAsStateWithLifecycle()
            GitHubIssueFinderTheme(issueMode = issueMode) {
                var showSplash by remember { mutableStateOf(true) }
                if (showSplash) {
                    // Full animation on the first launch, a short version that
                    // starts at the settled title on every launch after — decided once
                    // per launch, see StartupSplashViewModel.
                    val splashViewModel: StartupSplashViewModel = viewModel()
                    val onFinished = {
                        splashViewModel.onSplashFinished()
                        showSplash = false
                    }
                    // Which splash plays, and with what parameters, lives
                    // in ONE place now — AppStartSplashScreen.kt — rather
                    // than switching on BuildConfig.SPLASH_ANIMATION here
                    // directly, so the "production" flavor's "Play App
                    // Start Animation" replay (see TopReposScreen.kt) can
                    // never drift from what a real user actually sees at
                    // launch.
                    AppStartSplashScreen(
                        onFinished = onFinished,
                        animation = splashViewModel.splashAnimation,
                        playFullAnimation = splashViewModel.playFullAnimation,
                    )
                } else {
                    // The Discover flow.
                    DiscoverNavHost()
                }
            }
        }
    }
}
