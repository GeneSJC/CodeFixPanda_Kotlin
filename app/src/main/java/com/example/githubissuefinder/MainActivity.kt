package com.example.githubissuefinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.githubissuefinder.data.RepoBookmarks
import com.example.githubissuefinder.ui.discover.DiscoverNavHost
import com.example.githubissuefinder.ui.discover.IssueMode
import com.example.githubissuefinder.ui.navigation.AppNavHost
import com.example.githubissuefinder.ui.splash.OrigamiSplashScreen
import com.example.githubissuefinder.ui.splash.ShapeStackSplashScreen
import com.example.githubissuefinder.ui.splash.SplashScreen
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
        setContent {
            // App-wide Issue Mode (v0.41): read directly from the shared
            // IssueMode object (see IssueMode.kt) at this single root
            // theme call, which wraps the whole app — splash AND
            // whichever landing experience is active. Null (classic flow,
            // or repoFirst before a mode's been chosen) falls back to the
            // original light/dark scheme unchanged.
            GitHubIssueFinderTheme(issueMode = IssueMode.current) {
                var showSplash by remember { mutableStateOf(true) }
                if (showSplash) {
                    val onFinished = { showSplash = false }
                    // Which splash plays is set via BuildConfig.SPLASH_ANIMATION:
                    // "coin", "origami" (the real diamond-fold sequence), or
                    // "shapeStack" (the stop-motion layered shapes animation)
                    // — three independently selectable options, not variants
                    // of one another (v0.33 split "origami" and "shapeStack"
                    // back apart after they were briefly, mistakenly merged
                    // in v0.32).
                    when (BuildConfig.SPLASH_ANIMATION) {
                        "origami" -> OrigamiSplashScreen(onFinished = onFinished)
                        "shapeStack" -> ShapeStackSplashScreen(onFinished = onFinished)
                        else -> SplashScreen(onFinished = onFinished)
                    }
                } else {
                    // Which post-splash experience shows is set in
                    // app/build.gradle.kts via BuildConfig.LANDING_EXPERIENCE
                    // ("classic" or "repoFirst") — a build-time config value,
                    // same "branch by abstraction" pattern as SPLASH_ANIMATION,
                    // deliberately not a git branch: this lets the existing
                    // "classic" flow (AppNavHost, FeedTabsScreen, Bookmarks —
                    // everything working today) stay completely untouched and
                    // reachable at all times, while "repoFirst" is built up
                    // incrementally alongside it.
                    //
                    // "repoFirst" is the Discover flow (option C from the
                    // architecture discussion): a ranked Top Repos landing
                    // screen. As of v0.38, tapping a repo opens a real
                    // repo-scoped issue list (RepoIssuesScreen) with proper
                    // back-navigation, via DiscoverNavHost — no longer a
                    // no-op. Feed/Insights/Bookmarks scoping to the chosen
                    // repo remains a future Commit Milestone.
                    when (BuildConfig.LANDING_EXPERIENCE) {
                        "repoFirst" -> DiscoverNavHost()
                        else -> AppNavHost()
                    }
                }
            }
        }
    }
}
