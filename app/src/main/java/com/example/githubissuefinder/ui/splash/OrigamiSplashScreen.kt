package com.example.githubissuefinder.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private val DIAMOND_WIDTH = 160.dp
private val DIAMOND_HALF_HEIGHT = 80.dp
private const val HOLD_FLAT_MS = 900L
private const val FOLD_MS = 700
private const val PAUSE_MS = 200L

/**
 * A one-shot brand animation replicating the first two real folds shared
 * by the origami bird/crane/boat sequences referenced when building
 * this: a diamond (a square, drawn lying on its side) folds in half
 * along its horizontal midline into a triangle, then that triangle
 * folds in half again along its vertical midline into a smaller
 * triangle. Deliberately stops there — the crane's later squash-folds
 * (corners tucking INTO the model) are a much harder class of problem
 * and weren't attempted, per the explicit "just start it" scope.
 *
 * RESTORED IN v0.33: this was briefly, mistakenly overwritten in v0.32
 * by what's now ShapeStackSplashScreen.kt — that was meant to be a
 * separate, additional animation, not a replacement for this one. Both
 * now exist as independently selectable options; see MainActivity.kt
 * and TopReposScreen.kt's hamburger menu.
 *
 * Also in v0.33: added a HOLD_FLAT_MS pause before fold 1 begins, so the
 * flat, unfolded diamond is visible for a beat before folding starts —
 * direct feedback that the fold previously began too abruptly.
 *
 * WHY THIS AVOIDS THE RISK THE PREVIOUS TWO ATTEMPTS HAD: both earlier
 * versions needed to combine a rotation around one axis with a
 * DIFFERENT static rotation on the same layer, which isn't something
 * this project could verify composes correctly without seeing it
 * render. This version sidesteps that entirely — every shape is drawn
 * ALREADY oriented as a diamond (not as a square that then gets
 * rotated), so both fold hinges are naturally horizontal or vertical
 * from the start. Fold 1 is a plain rotationX around a horizontal
 * hinge; fold 2 is a plain rotationY around a vertical hinge on the
 * result of fold 1. Neither needs a compound rotation on one layer —
 * each is exactly as simple as the coin flip's own single rotation.
 *
 * Three pieces, each independently simple:
 *  - topHalf: the diamond's top triangle. Animates rotationX (0->180°)
 *    during fold 1, hinged at its own bottom edge, then stays frozen.
 *  - bottomLeftHalf: the LEFT half of the diamond's bottom triangle.
 *    Never animates — it's the stationary reference for fold 2.
 *  - bottomRightHalf: the RIGHT half of the diamond's bottom triangle.
 *    Animates rotationY (0->180°) during fold 2, hinged at its own
 *    left edge (the shared vertical centerline).
 * Composed in that order (topHalf first) so it renders BELOW the
 * bottom pieces — during fold 1 the two halves barely overlap until
 * the very end, so this doesn't hide fold 1, and it means fold 2's
 * rotation (on top) stays visible instead of being covered by the
 * now-frozen top piece.
 *
 * Selectable via BuildConfig.SPLASH_ANIMATION ("origami"). Built
 * entirely with core Compose APIs (Canvas + Path, graphicsLayer
 * rotationX/rotationY, Animatable) — no third-party dependency.
 *
 * Exact corner alignment between pieces is a reasonable-effort match
 * to the reference diagrams, not pixel-verified against a running
 * device — treat as a likely candidate for a small follow-up tuning
 * pass, same caveat every splash variant in this project has had.
 */
@Composable
fun OrigamiSplashScreen(onFinished: () -> Unit) {
    val density = LocalDensity.current
    val fold1 = remember { Animatable(0f) }
    val fold2 = remember { Animatable(0f) }
    val revealAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Hold on the flat, unfolded diamond for a beat before folding
        // begins — added per feedback that the fold started too abruptly.
        delay(HOLD_FLAT_MS)
        // Fold 1: diamond -> triangle.
        fold1.animateTo(180f, animationSpec = tween(durationMillis = FOLD_MS, easing = LinearOutSlowInEasing))
        delay(PAUSE_MS)
        // Fold 2: triangle -> smaller triangle.
        fold2.animateTo(180f, animationSpec = tween(durationMillis = FOLD_MS, easing = LinearOutSlowInEasing))
        delay(PAUSE_MS)
        // Brand mark + app name fade in together, then hold before handoff.
        revealAlpha.animateTo(1f, animationSpec = tween(durationMillis = 450))
        delay(2800)
        onFinished()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // The final folded shape lands in the LEFT half of this box
            // (bottomRightHalf folds ONTO the stationary bottomLeftHalf),
            // so the whole assembly is shifted left by a quarter-width to
            // bring that final shape's own center to the screen center.
            Box(
                modifier = Modifier
                    .size(width = DIAMOND_WIDTH, height = DIAMOND_HALF_HEIGHT * 2)
                    .offset(x = -(DIAMOND_WIDTH / 4)),
            ) {
                val primary = MaterialTheme.colorScheme.primary
                val primaryDim = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)

                // topHalf: apex at top, base at its own bottom edge (the hinge).
                Canvas(
                    modifier = Modifier
                        .size(width = DIAMOND_WIDTH, height = DIAMOND_HALF_HEIGHT)
                        .offset(x = 0.dp, y = 0.dp)
                        .graphicsLayer {
                            rotationX = fold1.value
                            transformOrigin = TransformOrigin(0.5f, 1f)
                            cameraDistance = 16f * density.density
                        },
                ) {
                    val path = Path().apply {
                        moveTo(size.width / 2f, 0f)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(path, color = primary)
                }

                // bottomLeftHalf: static, half the width, right-triangle shape.
                Canvas(
                    modifier = Modifier
                        .size(width = DIAMOND_WIDTH / 2, height = DIAMOND_HALF_HEIGHT)
                        .offset(x = 0.dp, y = DIAMOND_HALF_HEIGHT),
                ) {
                    val path = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width, 0f)
                        lineTo(size.width, size.height)
                        close()
                    }
                    drawPath(path, color = primaryDim)
                }

                // bottomRightHalf: animates rotationY during fold 2, hinged at its own left edge.
                Canvas(
                    modifier = Modifier
                        .size(width = DIAMOND_WIDTH / 2, height = DIAMOND_HALF_HEIGHT)
                        .offset(x = DIAMOND_WIDTH / 2, y = DIAMOND_HALF_HEIGHT)
                        .graphicsLayer {
                            rotationY = fold2.value
                            transformOrigin = TransformOrigin(0f, 0f)
                            cameraDistance = 16f * density.density
                        },
                ) {
                    val path = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width, 0f)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(path, color = primary)
                }

                // Brand mark centered on bottomLeftHalf's own footprint —
                // where the folded result actually ends up, not the
                // diamond's original full width.
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .offset(
                            x = DIAMOND_WIDTH / 4 - 28.dp,
                            y = DIAMOND_HALF_HEIGHT + (DIAMOND_HALF_HEIGHT / 2) - 28.dp,
                        ),
                ) {
                    SplashBrandMark(
                        modifier = Modifier.graphicsLayer { alpha = revealAlpha.value },
                        size = 56.dp,
                    )
                }
            }

            Text(
                "GitHubIssueFinder",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .graphicsLayer { alpha = revealAlpha.value },
            )
        }
    }
}
