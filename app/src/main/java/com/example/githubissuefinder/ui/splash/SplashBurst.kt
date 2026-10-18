package com.example.githubissuefinder.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

// Scaled with the text (see SPLASH_TEXT_SCALE) so the burst keeps its proportions relative to the
// bigger wordmark.
private val BURST_SIZE = (120 * SPLASH_TEXT_SCALE).dp
private const val BURST_START_SCALE = 0.4f
private const val BURST_MAX_SCALE = 2.8f
private const val BURST_START_ALPHA = 0.85f
private const val BURST_HOLD_FRACTION = 0.45

/**
 * The "sonic boom": a soft radial pulse that grows and fades behind the wordmark. Create one with
 * remember, draw it with [SplashBurst], and call [play] when it should fire.
 */
internal class BurstAnimation {
    internal val scale = Animatable(BURST_START_SCALE)
    internal val alpha = Animatable(0f)

    /**
     * Grows the pulse and fades it out over [durationMs]. Alpha holds near its start for the first
     * part: fading and scaling from the start left the circle nearly transparent by the time it was
     * big enough to see. [fadeOut] shapes the fade: an ease-in keeps the pulse visible until close to
     * the end, so a caller that continues the moment this returns does not wait on an invisible tail.
     */
    suspend fun play(durationMs: Int, fadeOut: Easing = LinearEasing) {
        coroutineScope {
            launch {
                scale.animateTo(BURST_MAX_SCALE, tween(durationMs, easing = FastOutSlowInEasing))
            }
            alpha.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = durationMs
                    BURST_START_ALPHA at 0
                    BURST_START_ALPHA at (durationMs * BURST_HOLD_FRACTION).toInt() using fadeOut
                    0f at durationMs
                },
            )
        }
    }
}

/** Draws [burst]. Declare it before the wordmark in a Box so it sits behind the text. */
@Composable
internal fun SplashBurst(burst: BurstAnimation) {
    Box(
        modifier = Modifier
            .size(BURST_SIZE)
            .graphicsLayer {
                scaleX = burst.scale.value
                scaleY = burst.scale.value
                alpha = burst.alpha.value
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
}
