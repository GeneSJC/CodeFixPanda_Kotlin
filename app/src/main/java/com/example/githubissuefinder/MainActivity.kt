package com.example.githubissuefinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
                    // Which splash plays is set in app/build.gradle.kts via
                    // BuildConfig.SPLASH_ANIMATION ("coin" or "origami") — a
                    // build-time config value, same pattern as CM_VERSION and
                    // GITHUB_API_TOKEN, not a runtime user-facing setting.
                    when (BuildConfig.SPLASH_ANIMATION) {
                        "origami" -> OrigamiSplashScreen(onFinished = onFinished)
                        else -> SplashScreen(onFinished = onFinished)
                    }
                } else {
                    AppNavHost()
                }
            }
        }
    }
}
