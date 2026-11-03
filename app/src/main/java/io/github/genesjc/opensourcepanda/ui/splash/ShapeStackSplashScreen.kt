package io.github.genesjc.opensourcepanda.ui.splash

import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.genesjc.opensourcepanda.R
import io.github.genesjc.opensourcepanda.model.IssueLabelFilter
import io.github.genesjc.opensourcepanda.ui.theme.ShapeStackBlue
import io.github.genesjc.opensourcepanda.ui.theme.ShapeStackGreen
import io.github.genesjc.opensourcepanda.ui.theme.ShapeStackPurple
import io.github.genesjc.opensourcepanda.ui.theme.colorScheme
import kotlinx.coroutines.delay

private const val APPEAR_MS = 400
private const val HOLD_MS = 1400L

/**
 * A "stop-motion" brand animation: a stack of colored shapes appears one at a time, each fading and
 * scaling in at a fixed offset and rotation and staying visible under the next, ending on the shared
 * "OSP" mark and app name. It leans into the snap-hold-snap rhythm that came out of tuning the origami
 * folds, and is a separate option from that animation. Only alpha and scale animate; positions and
 * rotations are constants, so there is no rotation-axis risk. The runtime (about 7 to 8 seconds) is
 * intentionally longer than the usual target, since shorter holds would lose its clunky-but-charming
 * quality. Core Compose APIs only; the shape layout is a reasonable-effort one.
 */
@Composable
fun ShapeStackSplashScreen(onFinished: () -> Unit) {
    val shape1Progress = remember { Animatable(0f) }
    val shape2Progress = remember { Animatable(0f) }
    val shape3Progress = remember { Animatable(0f) }
    val markAlpha = remember { Animatable(0f) }
    val nameAlpha = remember { Animatable(0f) }

    // One script, top to bottom: each animateTo() suspends until that piece has finished appearing and each
    // delay() is the hold before the next one, so the order of these lines is the order on screen.
    // onFinished() runs after the last hold; leaving the screen early cancels the effect and skips it.
    LaunchedEffect(Unit) {
        shape1Progress.animateTo(1f, animationSpec = tween(APPEAR_MS))
        delay(HOLD_MS)
        shape2Progress.animateTo(1f, animationSpec = tween(APPEAR_MS))
        delay(HOLD_MS)
        shape3Progress.animateTo(1f, animationSpec = tween(APPEAR_MS))
        delay(HOLD_MS)
        markAlpha.animateTo(1f, animationSpec = tween(APPEAR_MS))
        delay(500)
        nameAlpha.animateTo(1f, animationSpec = tween(APPEAR_MS))
        delay(2200)
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
            Box(modifier = Modifier.size(220.dp)) {
                // Shape 1: brand green.
                StackedTriangle(
                    color = ShapeStackGreen,
                    progress = shape1Progress.value,
                    modifier = Modifier
                        .size(140.dp)
                        .offset(x = 30.dp, y = 30.dp),
                )
                // Shape 2: purple, offset down-right, tilted — leaves a
                // corner of shape 1 visible behind/above it.
                StackedTriangle(
                    color = ShapeStackPurple,
                    progress = shape2Progress.value,
                    rotationDegrees = 18f,
                    modifier = Modifier
                        .size(110.dp)
                        .offset(x = 75.dp, y = 60.dp),
                )
                // Shape 3: blue, offset again, tilted the other way — leaves
                // slivers of both earlier shapes visible.
                StackedTriangle(
                    color = ShapeStackBlue,
                    progress = shape3Progress.value,
                    rotationDegrees = -14f,
                    modifier = Modifier
                        .size(90.dp)
                        .offset(x = 50.dp, y = 90.dp),
                )
                // Final frame: the shared brand mark, roughly centered
                // over the accumulated shape stack.
                Box(modifier = Modifier.offset(x = 72.dp, y = 72.dp)) {
                    SplashBrandMark(
                        modifier = Modifier.graphicsLayer { alpha = markAlpha.value },
                        size = 64.dp,
                    )
                }
            }

            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .graphicsLayer { alpha = nameAlpha.value },
            )
        }
    }
    }
}

/**
 * A single static (non-folding) triangle, fading and growing in from progress=0 to progress=1.
 * rotationDegrees is a fixed tilt, not animated, so nothing else rotates on its layer.
 */
@Composable
private fun StackedTriangle(
    color: Color,
    progress: Float,
    modifier: Modifier = Modifier,
    rotationDegrees: Float = 0f,
) {
    Canvas(
        modifier = modifier.graphicsLayer {
            alpha = progress
            scaleX = 0.7f + 0.3f * progress
            scaleY = 0.7f + 0.3f * progress
            rotationZ = rotationDegrees
        },
    ) {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width / 2f, size.height)
            close()
        }
        drawPath(path, color = color)
    }
}
