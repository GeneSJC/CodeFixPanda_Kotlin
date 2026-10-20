package io.github.genesjc.githubissuefinder.ui.splash

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.genesjc.githubissuefinder.R
import io.github.genesjc.githubissuefinder.model.IssueLabelFilter
import io.github.genesjc.githubissuefinder.ui.theme.colorScheme
import kotlinx.coroutines.delay

private val DIAMOND_WIDTH = 160.dp
private val DIAMOND_HALF_HEIGHT = 80.dp
private const val HOLD_FLAT_MS = 900L
private const val FOLD_MS = 700
private const val PAUSE_MS = 200L

/**
 * A one-shot brand animation of the first two folds of an origami crane: a diamond folds in half
 * along its horizontal midline into a triangle, then that triangle folds in half along its vertical
 * midline. It stops there; the crane's later squash-folds are a much harder problem. Every shape is
 * drawn already oriented as a diamond, so each fold is a plain single-axis rotation (rotationX around
 * a horizontal hinge, then rotationY around a vertical one) with no compound rotation on one layer.
 * Three pieces: topHalf (rotationX during fold 1), bottomLeftHalf (stationary) and bottomRightHalf
 * (rotationY during fold 2), composed in that order so the top piece renders below and does not cover
 * fold 2. A pause before fold 1 shows the flat diamond for a beat. Core Compose APIs only. Corner
 * alignment is a reasonable-effort match, not pixel-verified.
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

    // Pinned to the purple Good First Issue palette regardless of the
    // app's active Issue Mode — see TitleRevealSplashScreen's doc
    // comment for the full reasoning.
    MaterialTheme(colorScheme = IssueLabelFilter.GOOD_FIRST_ISSUE.colorScheme()) {
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
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .graphicsLayer { alpha = revealAlpha.value },
            )
        }
    }
    }
}
