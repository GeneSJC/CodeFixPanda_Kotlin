package com.example.githubissuefinder.ui.splash

import androidx.compose.runtime.Composable

/**
 * The single source of truth for "which splash actually plays at real
 * app launch, with exactly what parameters" — driven by the
 * [animation] argument (AppConfig's splashAnimation, from
 * BuildConfig.SPLASH_ANIMATION, supplied by the caller's ViewModel): "coin", "origami" (the diamond-fold
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
 *
 * [playFullAnimation] false swaps the configured animation for
 * TitleFlashSplashScreen, a short fade-in of the app name: a real launch
 * passes false once the device has already seen the full animation
 * finish (see StartupSplashViewModel), because the full animation is
 * fun once and a bit much on repeat. The default, true, is what the
 * in-app "Play App Start Animation" replay uses, so a replay always
 * shows the full animation.
 */
@Composable
fun AppStartSplashScreen(onFinished: () -> Unit, animation: String, playFullAnimation: Boolean = true) {
    if (!playFullAnimation) {
        TitleFlashSplashScreen(onFinished = onFinished)
        return
    }
    when (animation) {
        "origami" -> OrigamiSplashScreen(onFinished = onFinished)
        "shapeStack" -> ShapeStackSplashScreen(onFinished = onFinished)
        "titleReveal" -> TitleRevealSplashScreen(onFinished = onFinished, speedMultiplier = 0.5f)
        else -> SplashScreen(onFinished = onFinished)
    }
}
