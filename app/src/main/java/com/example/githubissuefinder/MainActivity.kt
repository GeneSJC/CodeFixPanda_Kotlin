package com.example.githubissuefinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.githubissuefinder.ui.discover.TopReposScreen
import com.example.githubissuefinder.ui.navigation.AppNavHost
import com.example.githubissuefinder.ui.splash.OrigamiSplashScreen
import com.example.githubissuefinder.ui.splash.SplashScreen
import com.example.githubissuefinder.ui.theme.GitHubIssueFinderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GitHubIssueFinderTheme {
                var showSplash by remember { mutableStateOf(true) }
                if (showSplash) {
                    val onFinished = { showSplash = false }
                    // Which splash plays is set via BuildConfig.SPLASH_ANIMATION.
                    when (BuildConfig.SPLASH_ANIMATION) {
                        "origami" -> OrigamiSplashScreen(onFinished = onFinished)
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
                    // "repoFirst" is CM 1 of an in-progress redesign (option C
                    // from the architecture discussion): a ranked Top Repos
                    // landing screen. Tapping a repo is currently a no-op —
                    // scoped Feed/Insights/Bookmarks for a chosen repo are
                    // future Commit Milestones, built on top of this same
                    // config switch, not a replacement for it.
                    when (BuildConfig.LANDING_EXPERIENCE) {
                        "repoFirst" -> TopReposScreen(onRepoClick = { /* no-op for CM 1 */ })
                        else -> AppNavHost()
                    }
                }
            }
        }
    }
}
