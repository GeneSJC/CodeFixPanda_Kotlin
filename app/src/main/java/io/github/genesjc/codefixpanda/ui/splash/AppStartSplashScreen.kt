package io.github.genesjc.codefixpanda.ui.splash

import androidx.compose.runtime.Composable
import io.github.genesjc.codefixpanda.config.SplashAnimation

/**
 * The single source of truth for which splash plays at launch and with what parameters, driven by
 * [animation] (titleReveal plays at 0.5x speed).
 * MainActivity's cold-start splash calls it, and so does the "Play App Start Animation" menu entry,
 * so a replay can never drift from what a user actually saw. [playFullAnimation] false swaps in
 * TitleSettledSplashScreen, the title already settled and then the same burst and hold the full
 * animation ends with: a real launch passes false once the device has seen the full animation. The
 * default, true, is what the replay uses.
 */
@Composable
fun AppStartSplashScreen(onFinished: () -> Unit, animation: SplashAnimation, playFullAnimation: Boolean = true) {
    if (!playFullAnimation) {
        TitleSettledSplashScreen(onFinished = onFinished)
        return
    }

    when (animation) {
        SplashAnimation.COIN -> SplashScreen(onFinished = onFinished)
        SplashAnimation.ORIGAMI -> OrigamiSplashScreen(onFinished = onFinished)
        SplashAnimation.SHAPE_STACK -> ShapeStackSplashScreen(onFinished = onFinished)
        SplashAnimation.TITLE_REVEAL -> TitleRevealSplashScreen(onFinished = onFinished, speedMultiplier = 0.5f)
    }
}
