package io.github.genesjc.githubissuefinder.ui.discover

import androidx.compose.runtime.Composable
import io.github.genesjc.githubissuefinder.ui.splash.AppStartSplashScreen
import io.github.genesjc.githubissuefinder.ui.splash.OrigamiSplashScreen
import io.github.genesjc.githubissuefinder.ui.splash.ShapeStackSplashScreen
import io.github.genesjc.githubissuefinder.ui.splash.SplashScreen
import io.github.genesjc.githubissuefinder.ui.splash.TitleRevealSplashScreen

/**
 * The splash animations the Top Repos menu can replay: one entry per animation in the detailed
 * (dev) menu, or just APP_START, which replays exactly what a real launch shows.
 */
internal enum class AnimationReplay {
    ORIGAMI,
    SHAPE_STACK,
    COIN,
    TITLE_REVEAL,
    TITLE_REVEAL_HALF_SPEED,
    APP_START,
}

/** Plays [animation] over the screen, then calls [onFinished]. [splashAnimation] is the configured launch animation. */
@Composable
internal fun AnimationReplayOverlay(animation: AnimationReplay, splashAnimation: String, onFinished: () -> Unit) {
    when (animation) {
        AnimationReplay.ORIGAMI -> OrigamiSplashScreen(onFinished = onFinished)
        AnimationReplay.SHAPE_STACK -> ShapeStackSplashScreen(onFinished = onFinished)
        AnimationReplay.COIN -> SplashScreen(onFinished = onFinished)
        AnimationReplay.TITLE_REVEAL -> TitleRevealSplashScreen(onFinished = onFinished)
        AnimationReplay.TITLE_REVEAL_HALF_SPEED -> TitleRevealSplashScreen(onFinished = onFinished, speedMultiplier = 0.5f)
        AnimationReplay.APP_START -> AppStartSplashScreen(onFinished = onFinished, animation = splashAnimation)
    }
}
