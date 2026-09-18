package com.example.githubissuefinder.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val PANEL_WIDTH = 48.dp
private val PANEL_HEIGHT = 140.dp
private const val PANEL_UNFOLD_MS = 500
private const val PANEL_STAGGER_MS = 260L

/**
 * A one-shot ~5 second brand animation: three vertical panels start
 * folded edge-on (rotationY = 90°, like a collapsed accordion or closed
 * blinds), then unfold left-to-right in an overlapping staggered wave to
 * their flat resting position — after which the shared "GH" brand mark
 * (see SplashBrandMark.kt) and app name fade in on top.
 *
 * Selectable via BuildConfig.SPLASH_ANIMATION ("origami") as an
 * alternative to the coin-flip SplashScreen — see MainActivity.kt.
 *
 * Simplified from a literal multi-directional origami fold (creases
 * opening from every edge at once) to a single-axis left-to-right
 * accordion unfold: more robust to implement correctly without being
 * able to visually test it first, while keeping the same "unfolds panel
 * by panel" spirit from the original idea. Built entirely with core
 * Compose APIs (graphicsLayer rotationY + cameraDistance, Animatable) —
 * same toolkit as the coin flip, no third-party dependency.
 *
 * Panel sizing/timing is a reasonable starting point, not visually
 * tuned against a real device — expect a possible follow-up adjustment,
 * same caveat as the coin flip.
 */
@Composable
fun OrigamiSplashScreen(onFinished: () -> Unit) {
    val density = LocalDensity.current
    val panelRotations = remember { List(3) { Animatable(90f) } }
    val revealAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Phase 1: panels unfold left-to-right in an overlapping wave —
        // each starts before the previous one fully finishes, for a
        // fluid cascade rather than strict one-at-a-time.
        coroutineScope {
            panelRotations.forEachIndexed { index, anim ->
                launch {
                    delay(index * PANEL_STAGGER_MS)
                    anim.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = PANEL_UNFOLD_MS, easing = LinearOutSlowInEasing),
                    )
                }
            }
        }
        // Phase 2: brief pause, then the brand mark + app name fade in together.
        delay(200)
        revealAlpha.animateTo(1f, animationSpec = tween(durationMillis = 450))
        // Phase 3: hold, then hand off.
        delay(3300)
        onFinished()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Row {
                    panelRotations.forEachIndexed { index, anim ->
                        val panelAlpha = if (index % 2 == 0) 1f else 0.85f
                        Box(
                            modifier = Modifier
                                .size(width = PANEL_WIDTH, height = PANEL_HEIGHT)
                                .graphicsLayer {
                                    rotationY = anim.value
                                    transformOrigin = TransformOrigin(0f, 0.5f)
                                    cameraDistance = 16f * density.density
                                }
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = panelAlpha)),
                        )
                    }
                }

                SplashBrandMark(
                    modifier = Modifier.graphicsLayer { alpha = revealAlpha.value },
                    size = 72.dp,
                )
            }

            Text(
                "GitHubIssueFinder",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .graphicsLayer { alpha = revealAlpha.value },
            )
        }
    }
}
