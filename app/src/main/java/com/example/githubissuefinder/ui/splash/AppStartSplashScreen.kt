package com.example.githubissuefinder.ui.splash

import androidx.compose.runtime.Composable

/**
 * The single source of truth for which splash plays at launch and with what parameters, driven by
 * [animation] ("coin", "origami", "shapeStack" or "titleReveal", the last at 0.5x speed).
 * MainActivity's cold-start splash calls it, and so does the "Play App Start Animation" menu entry,
 * so a replay can never drift from what a user actually saw. [playFullAnimation] false swaps in
 * TitleFlashSplashScreen, the app name with a short burst: a real launch passes false once the
 * device has seen the full animation. The default, true, is what the replay uses. [onHandoff] is
 * called shortly before the short splash ends, so the app can start appearing underneath while it
 * fades out; the full animations never call it, and the caller shows the app when [onFinished]
 * arrives.
 */
@Composable
fun AppStartSplashScreen(
    onFinished: () -> Unit,
    animation: String,
    playFullAnimation: Boolean = true,
    onHandoff: () -> Unit = {},
) {
    if (!playFullAnimation) {
        TitleFlashSplashScreen(onFinished = onFinished, onHandoff = onHandoff)
        return
    }
    when (animation) {
        "origami" -> OrigamiSplashScreen(onFinished = onFinished)
        "shapeStack" -> ShapeStackSplashScreen(onFinished = onFinished)
        "titleReveal" -> TitleRevealSplashScreen(onFinished = onFinished, speedMultiplier = 0.5f)
        else -> SplashScreen(onFinished = onFinished)
    }
}
