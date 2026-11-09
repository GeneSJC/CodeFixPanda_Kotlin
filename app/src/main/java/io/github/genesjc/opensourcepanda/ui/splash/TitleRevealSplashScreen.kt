package io.github.genesjc.opensourcepanda.ui.splash

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.genesjc.opensourcepanda.ui.theme.BrandColorScheme

// Scaled with the text (see SPLASH_TEXT_SCALE) so the burst keeps its
// proportions relative to the bigger wordmark.
private val BURST_SIZE = (120 * SPLASH_TEXT_SCALE).dp

/**
 * A kinetic title-reveal splash: "Code" slides in from the left and overshoots before settling, "Fix"
 * slides in from the right and shudders on impact, "PANDA" rises into place beneath them, and finally the
 * panda drops in above the name and lands with a squash while a burst of light flashes behind it.
 * [speedMultiplier] scales every stage duration (0.5 means twice as slow), so one composable serves
 * the standard and half-speed replays. The timing and the animated values are TitleRevealTimeline's; this
 * draws what they say. The text is drawn by TitleWordmark, shared with the short fade-in splash.
 */
@Composable
fun TitleRevealSplashScreen(onFinished: () -> Unit, speedMultiplier: Float = 1f) {
    val density = LocalDensity.current
    val timeline = remember(speedMultiplier) { TitleRevealTimeline(density, speedMultiplier) }

    // The whole wordmark stays invisible until the timeline is about to
    // start, as a second guard: even if some value were ever wrong for a
    // frame, nothing is drawn until `started` flips.
    var started by remember { mutableStateOf(false) }

    LaunchedEffect(timeline) {
        // Let two blank frames go by BEFORE starting any timer or animation: the first frame of a cold start
        // can be very slow (class loading, first-time Compose set-up), and a timeline started inside that
        // hitch would stutter its opening.
        withFrameNanos { }
        withFrameNanos { }
        started = true
        timeline.play(onFinished)
    }

    // Pinned to the brand palette regardless of the app's currently active Issue Mode: the splash is the
    // app's own fixed identity (see SplashBrandMark's doc comment), not something that should repaint
    // itself green under Help Wanted because Theme.kt's root MaterialTheme call is mode-aware.
    MaterialTheme(colorScheme = BrandColorScheme) {
        SplashBackground {
            TitleWordmark(
                modifier = Modifier.graphicsLayer { alpha = if (started) 1f else 0f },
                openModifier = Modifier.graphicsLayer {
                    translationX = timeline.openOffsetX.value
                    rotationZ = timeline.openRotation.value
                },
                sourceModifier = Modifier.graphicsLayer { translationX = timeline.sourceOffsetX.value },
                pandaModifier = Modifier.graphicsLayer {
                    translationY = timeline.pandaOffsetY.value
                    alpha = timeline.pandaAlpha.value
                },
                mascotModifier = Modifier.graphicsLayer {
                    translationY = timeline.mascotOffsetY.value
                    alpha = timeline.mascotAlpha.value
                    scaleX = timeline.mascotScaleX.value
                    scaleY = timeline.mascotScaleY.value
                    transformOrigin = TransformOrigin(0.5f, 1f)
                },
                mascotBackdrop = {
                    SplashBurst(BURST_SIZE / 2, burstScale = { timeline.burstScale.value }, burstAlpha = { timeline.burstAlpha.value })
                },
            )
        }
    }
}
