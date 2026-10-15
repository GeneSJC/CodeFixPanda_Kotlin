package com.example.githubissuefinder

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
import com.example.githubissuefinder.data.HiddenIssuesStore
import com.example.githubissuefinder.data.IssueModeRepository
import com.example.githubissuefinder.data.RepoBookmarks
import com.example.githubissuefinder.data.StartupSplashRepository
import com.example.githubissuefinder.ui.discover.DiscoverNavHost
import com.example.githubissuefinder.ui.discover.IssueModeViewModel
import com.example.githubissuefinder.ui.splash.AppStartSplashScreen
import com.example.githubissuefinder.ui.splash.StartupSplashViewModel
import com.example.githubissuefinder.ui.theme.GitHubIssueFinderTheme

/**
 * `android:configChanges` in the manifest (orientation/screenSize/
 * screenLayout/smallestScreenSize/keyboardHidden/uiMode) tells the
 * system NOT to destroy and recreate this Activity on rotation —
 * added after two real, confirmed-on-device bugs traced back to the
 * same root cause: without it, Android's default behavior fully
 * destroys and recreates the Activity on every rotation, which
 * (1) reset `showSplash` below back to `true` on every rotation,
 * replaying the start animation on an already-running app, not just
 * at real cold start, and (2) was the suspected cause of a white
 * screen the app couldn't be navigated out of after rotating into
 * landscape — likely some piece of state (Navigation Compose's own
 * back stack among the candidates) not surviving that
 * destroy/recreate cycle cleanly. With this flag set, Compose instead
 * just recomposes in place on a configuration change — which is
 * already exactly how TopReposScreen's own landscape detection
 * (LocalConfiguration.current.orientation) expects to work.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // v0.44: restore any previously-chosen Issue Mode BEFORE
        // setContent, so it's already in place by the time
        // DiscoverNavHost computes its startDestination on first
        // composition — see IssueModeRepository's own doc comment.
        IssueModeRepository.init(this)
        // v0.48: same reasoning, for the repo-first flow's persisted
        // bookmarks — see RepoBookmarks.init()'s own doc comment.
        RepoBookmarks.init(this)
        // v0.51: same reasoning, for the repo-first flow's persisted
        // hidden-issue store — see HiddenIssuesStore.init()'s own doc
        // comment.
        HiddenIssuesStore.init(this)
        // Whether this device has already seen the full start animation —
        // same reasoning, see StartupSplashViewModel.
        StartupSplashRepository.init(this)
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
                    // Full animation on the first launch, a short fade-in
                    // of the app name on every launch after — decided once
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
                        playFullAnimation = splashViewModel.playFullAnimation,
                    )
                } else {
                    // The Discover flow (option C from the architecture
                    // discussion): a ranked Top Repos landing screen.
                    // Tapping a repo opens a real repo-scoped issue list
                    // (RepoIssuesScreen) with proper back-navigation, via
                    // DiscoverNavHost. This was formerly one of two
                    // landing experiences selectable via
                    // BuildConfig.LANDING_EXPERIENCE — the other,
                    // "classic" (AppNavHost/FeedTabsScreen/Bookmarks), was
                    // removed once this flow became the sole supported
                    // experience.
                    DiscoverNavHost()
                }
            }
        }
    }
}
