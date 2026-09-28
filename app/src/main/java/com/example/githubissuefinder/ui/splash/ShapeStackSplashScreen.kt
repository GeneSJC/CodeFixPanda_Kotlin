package com.example.githubissuefinder.ui.splash

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
import androidx.compose.ui.unit.dp
import com.example.githubissuefinder.model.IssueLabelFilter
import com.example.githubissuefinder.ui.theme.colorScheme
import kotlinx.coroutines.delay

private const val APPEAR_MS = 400
private const val HOLD_MS = 1400L

/**
 * A "stop-motion" brand animation: a stack of colored shapes appear one
 * at a time — each fading/scaling in at a fixed, slightly offset and
 * rotated position, staying visible as the next one lands partially over
 * it — followed by the shared "GH" brand mark and app name as the final
 * frame.
 *
 * ORIGIN OF THIS DESIGN: while iterating on OrigamiSplashScreen's real
 * fold sequence (diamond -> triangle -> smaller triangle), the tween
 * easing plus the pause between folds produced a distinct "snap into a
 * pose, hold, snap to the next" rhythm — closer to classic cel animation
 * (draw a frame, hold it, draw the next) than to smooth continuous
 * folding. That rhythm was explicitly liked on its own terms, so this is
 * a SEPARATE animation built to lean into it directly, rather than a
 * replacement for the fold-based one — both now exist independently
 * (see MainActivity.kt and TopReposScreen.kt's hamburger menu). An
 * earlier version of this project briefly (and mistakenly) put this
 * content directly into OrigamiSplashScreen.kt itself, overwriting the
 * fold sequence; that was corrected in v0.33, splitting them back into
 * two separate files/options.
 *
 * Each shape's position/rotation is a fixed constant, not animated — the
 * ONLY animated properties are alpha and scale (a simple fade+grow-in),
 * which is why this carries none of the rotation-axis-composition risk
 * the fold-based animation needed to reason through. As low-risk as the
 * original coin flip's own single rotation.
 *
 * Total runtime is intentionally longer than the ~5 second target used
 * elsewhere (roughly 7-8 seconds) — compressing the hold times back down
 * would undercut the exact "cartoon-y, clunky-but-charming" quality that
 * was the whole point of this direction.
 *
 * Selectable via BuildConfig.SPLASH_ANIMATION ("shapeStack"). Built
 * entirely with core Compose APIs (Canvas + Path, graphicsLayer,
 * Animatable) — no third-party dependency. Exact shape positions are a
 * reasonable-effort layout, not visually tuned against a running
 * device — same caveat every splash variant in this project has had.
 */
@Composable
fun ShapeStackSplashScreen(onFinished: () -> Unit) {
    val shape1Progress = remember { Animatable(0f) }
    val shape2Progress = remember { Animatable(0f) }
    val shape3Progress = remember { Animatable(0f) }
    val markAlpha = remember { Animatable(0f) }
    val nameAlpha = remember { Animatable(0f) }

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
                    color = Color(0xFF0F6E56),
                    progress = shape1Progress.value,
                    modifier = Modifier
                        .size(140.dp)
                        .offset(x = 30.dp, y = 30.dp),
                )
                // Shape 2: purple, offset down-right, tilted — leaves a
                // corner of shape 1 visible behind/above it.
                StackedTriangle(
                    color = Color(0xFF534AB7),
                    progress = shape2Progress.value,
                    rotationDegrees = 18f,
                    modifier = Modifier
                        .size(110.dp)
                        .offset(x = 75.dp, y = 60.dp),
                )
                // Shape 3: blue, offset again, tilted the other way — leaves
                // slivers of both earlier shapes visible.
                StackedTriangle(
                    color = Color(0xFF0B5FA5),
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
                "GitHubIssueFinder",
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
 * A single static (non-folding) triangle, fading and growing in from
 * progress=0 to progress=1. rotationDegrees is a fixed cosmetic tilt,
 * not animated — safe on its own since nothing else rotates on the same
 * layer at the same time.
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
