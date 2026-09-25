package com.example.githubissuefinder.ui.splash

import androidx.compose.runtime.Composable
import com.example.githubissuefinder.BuildConfig

/**
 * The single source of truth for "which splash actually plays at real
 * app launch, with exactly what parameters" — driven by
 * BuildConfig.SPLASH_ANIMATION: "coin", "origami" (the diamond-fold
 * sequence), "shapeStack" (the stop-motion layered shapes animation),
 * or "titleReveal" (the kinetic word-by-word title reveal, played at
 * 0.5x speed — confirmed on-device as giving it enough room to
 * register without feeling rushed).
 *
 * MainActivity's actual cold-start splash calls this directly. It also
 * backs the single "Play App Start Animation" entry TopReposScreen.kt's
 * hamburger menu falls back to when BuildConfig.SHOW_DETAILED_DRAWER_MENU
 * is false (see app/build.gradle.kts) — the default, so a fresh clone/
 * run shows this instead of the full per-animation replay toolkit.
 * Centralizing the dispatch here (rather than each call site separately
 * switching on BuildConfig.SPLASH_ANIMATION) is what
 * guarantees that replay can never drift from what a real user actually
 * saw at launch — e.g. it stays in sync with "titleReveal"'s
 * speedMultiplier without that value needing to be duplicated anywhere.
 */
@Composable
fun AppStartSplashScreen(onFinished: () -> Unit) {
    when (BuildConfig.SPLASH_ANIMATION) {
        "origami" -> OrigamiSplashScreen(onFinished = onFinished)
        "shapeStack" -> ShapeStackSplashScreen(onFinished = onFinished)
        "titleReveal" -> TitleRevealSplashScreen(onFinished = onFinished, speedMultiplier = 0.5f)
        else -> SplashScreen(onFinished = onFinished)
    }
}
