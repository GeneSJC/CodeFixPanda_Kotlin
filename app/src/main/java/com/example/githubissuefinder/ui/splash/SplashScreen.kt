package com.example.githubissuefinder.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.githubissuefinder.model.IssueLabelFilter
import com.example.githubissuefinder.ui.theme.colorScheme
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

private const val FLIGHT_DURATION_MS = 2200
// A whole-number multiple of 360° so the mark always lands right-side-up
// and legible, never stopped mid-rotation.
private const val TOTAL_SPIN_DEGREES = 1080f
private val STAGE_HEIGHT = 220.dp
private val TOSS_HEIGHT = 150.dp
private val COIN_SIZE = 96.dp

/**
 * A one-shot ~5 second brand animation shown before the main app: the
 * shared "GH" brand mark (see SplashBrandMark.kt) tosses into the air,
 * spins, falls, bounces once, and settles — then calls onFinished() to
 * hand off to the real app content.
 *
 * Selectable via BuildConfig.SPLASH_ANIMATION ("coin", the default) as
 * an alternative to OrigamiSplashScreen — see MainActivity.kt.
 *
 * NOT the system SplashScreen API. Android's built-in cold-start splash
 * is meant for brief, simple branding — Google's own guidance
 * discourages long custom animations there, and the OS can cut it
 * short. This is a custom first Compose screen instead: MainActivity
 * launches instantly, and this is simply the first thing shown, with
 * full control over timing and no OS constraints.
 *
 * Built entirely with core Compose animation APIs (graphicsLayer
 * rotationX + cameraDistance for the pseudo-3D flip, Animatable, spring
 * for the landing bounce) — no third-party library, no import
 * uncertainty. Exact spacing/sizing is a reasonable starting point, not
 * visually tuned against a real device.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val density = LocalDensity.current
    val flightProgress = remember { Animatable(0f) }
    val bounceScale = remember { Animatable(1f) }
    val nameAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Phase 1: toss up, spin, fall — a linear time-progress mapped
        // through sin() below, which naturally gives a physically
        // plausible ease-in/ease-out arc without needing a separate
        // easing curve.
        flightProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = FLIGHT_DURATION_MS, easing = LinearEasing),
        )
        // Phase 2: a quick squash on impact, then a bouncy spring settle.
        bounceScale.animateTo(0.82f, animationSpec = tween(120, easing = LinearEasing))
        bounceScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        )
        // Phase 3: app name fades in, brief hold, then hand off.
        nameAlpha.animateTo(1f, animationSpec = tween(durationMillis = 500))
        delay(1900)
        onFinished()
    }

    val progress = flightProgress.value
    // sin(progress * PI): 0 at start, 1 at the apex (midpoint), back to 0
    // at the end — a natural up-then-down arc from one formula, also
    // reused to drive the shadow's shrink/fade so both stay visually
    // connected to the same "how high is it right now" value.
    val heightFraction = sin((progress * PI).toFloat()).coerceIn(0f, 1f)

    // Pinned to the purple Good First Issue palette regardless of the
    // app's active Issue Mode — see TitleRevealSplashScreen's doc
    // comment for the full reasoning.
    MaterialTheme(colorScheme = IssueLabelFilter.GOOD_FIRST_ISSUE.colorScheme()) {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.height(STAGE_HEIGHT), contentAlignment = Alignment.BottomCenter) {
                // Shadow: fixed position (the "table"), shrinks and
                // lightens as the coin rises away from it.
                Box(
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .size((64 - 20 * heightFraction).dp)
                        .graphicsLayer { alpha = 0.16f * (1f - heightFraction * 0.5f) }
                        .background(Color.Black, CircleShape),
                )

                // The coin: rests just above the shadow at start/end, lifts during flight.
                SplashBrandMark(
                    modifier = Modifier.graphicsLayer {
                        translationY = -heightFraction * with(density) { TOSS_HEIGHT.toPx() }
                        rotationX = progress * TOTAL_SPIN_DEGREES
                        scaleX = bounceScale.value
                        scaleY = bounceScale.value
                        cameraDistance = 16f * density.density
                    },
                    size = COIN_SIZE,
                )
            }

            Text(
                "GitHubIssueFinder",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .graphicsLayer { alpha = nameAlpha.value },
            )
        }
    }
    }
}
