package com.example.githubissuefinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.githubissuefinder.data.HiddenIssuesStore
import com.example.githubissuefinder.data.RepoBookmarks
import com.example.githubissuefinder.ui.discover.DiscoverNavHost
import com.example.githubissuefinder.ui.discover.IssueMode
import com.example.githubissuefinder.ui.splash.AppStartSplashScreen
import com.example.githubissuefinder.ui.theme.GitHubIssueFinderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // v0.44: restore any previously-chosen Issue Mode BEFORE
        // setContent, so it's already in place by the time
        // DiscoverNavHost computes its startDestination on first
        // composition — see IssueMode.init()'s own doc comment.
        IssueMode.init(this)
        // v0.48: same reasoning, for the repo-first flow's persisted
        // bookmarks — see RepoBookmarks.init()'s own doc comment.
        RepoBookmarks.init(this)
        // v0.51: same reasoning, for the repo-first flow's persisted
        // hidden-issue store — see HiddenIssuesStore.init()'s own doc
        // comment.
        HiddenIssuesStore.init(this)
        setContent {
            // App-wide Issue Mode (v0.41): read directly from the shared
            // IssueMode object (see IssueMode.kt) at this single root
            // theme call, which wraps the whole app — splash and the
            // Discover flow alike. Null (no mode chosen yet) falls back
            // to the original light/dark scheme unchanged.
            GitHubIssueFinderTheme(issueMode = IssueMode.current) {
                var showSplash by remember { mutableStateOf(true) }
                if (showSplash) {
                    val onFinished = { showSplash = false }
                    // Which splash plays, and with what parameters, lives
                    // in ONE place now — AppStartSplashScreen.kt — rather
                    // than switching on BuildConfig.SPLASH_ANIMATION here
                    // directly, so the "production" flavor's "Play App
                    // Start Animation" replay (see TopReposScreen.kt) can
                    // never drift from what a real user actually sees at
                    // launch.
                    AppStartSplashScreen(onFinished = onFinished)
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
