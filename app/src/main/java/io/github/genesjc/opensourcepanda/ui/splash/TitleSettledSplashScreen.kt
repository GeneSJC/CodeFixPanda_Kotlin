package io.github.genesjc.opensourcepanda.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import io.github.genesjc.opensourcepanda.model.IssueLabelFilter
import io.github.genesjc.opensourcepanda.ui.theme.colorScheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// The burst look is carried over from TitleRevealSplashScreen. The burst is 25% slower than there
// (625 vs 500) with the hold lengthened to match, so the pause after it is unchanged. They are the
// BASE (1x) values; speedMultiplier scales them.
private const val HOLD_BASE_MS = 1025
private const val BURST_BASE_DURATION_MS = 625

// How long the settled title shows on its own before the burst begins.
private const val TITLE_ONLY_MS = 1350L


// Shaved off the end of the copied hold, which otherwise leaves a perceptible pause after the burst.
private const val END_TRIM_MS = 150L

// Scaled with the text (see SPLASH_TEXT_SCALE) so the burst keeps its
// proportions relative to the bigger wordmark.
private val BURST_SIZE = (120 * SPLASH_TEXT_SCALE).dp
private const val BURST_START_SCALE = 0.4f
private const val BURST_MAX_SCALE = 2.8f
private const val BURST_START_ALPHA = 0.85f
private const val BURST_HOLD_FRACTION = 0.45

/**
 * The short splash for every launch after the first: the title screen as the full animation leaves
 * it once settled, fading in until the burst ends, and on its own for [TITLE_ONLY_MS] before the burst, then the same "sonic boom" burst and hold
 * that animation ends with, at the 0.5x speed it uses at launch. This is an independent copy of
 * that ending, not shared code, so the first-launch animation is never at risk from changes here.
 * Like the original it lets two frames go by before starting, since the first frame of a cold start
 * can be slow. Pinned to the Good First Issue (purple) palette, because the splash is the
 * project's fixed brand identity.
 */
@Composable
fun TitleSettledSplashScreen(onFinished: () -> Unit, speedMultiplier: Float = 0.5f) {
    val burstDurationMs = (BURST_BASE_DURATION_MS / speedMultiplier).toInt()
    val holdMs = (HOLD_BASE_MS / speedMultiplier).toInt()
    val burstScale = remember { Animatable(BURST_START_SCALE) }
    val burstAlpha = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }

    LaunchedEffect(speedMultiplier) {
        // Let the first couple of frames go by before starting, so a slow cold-start frame does not
        // land in the middle of the burst.
        withFrameNanos { }
        withFrameNanos { }

        // Linear, across the whole wait and the burst, so the fade is visible the entire time.
        launch { titleAlpha.animateTo(1f, tween(TITLE_ONLY_MS.toInt() + burstDurationMs, easing = LinearEasing)) }
        delay(TITLE_ONLY_MS)

        // Alpha holds near its start for the first BURST_HOLD_FRACTION before fading; fading and
        // scaling from the start left the circle nearly transparent by the time it was big enough
        // to see.
        launch {
            burstScale.animateTo(BURST_MAX_SCALE, tween(burstDurationMs, easing = FastOutSlowInEasing))
        }
        launch {
            burstAlpha.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = burstDurationMs
                    BURST_START_ALPHA at 0
                    BURST_START_ALPHA at (burstDurationMs * BURST_HOLD_FRACTION).toInt()
                    0f at burstDurationMs
                },
            )
        }

        delay(holdMs - END_TRIM_MS)
        onFinished()
    }

    MaterialTheme(colorScheme = IssueLabelFilter.GOOD_FIRST_ISSUE.colorScheme()) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            // Drawn first so it sits behind the wordmark below.
            Box(
                modifier = Modifier
                    .size(BURST_SIZE)
                    .graphicsLayer {
                        scaleX = burstScale.value
                        scaleY = burstScale.value
                        alpha = burstAlpha.value
                    }
                    .background(
                        Brush.radialGradient(
                            0f to MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            0.6f to MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            1f to MaterialTheme.colorScheme.primary.copy(alpha = 0f),
                        ),
                        CircleShape,
                    ),
            )

            TitleWordmark(modifier = Modifier.graphicsLayer { alpha = titleAlpha.value })
        }
    }
}
