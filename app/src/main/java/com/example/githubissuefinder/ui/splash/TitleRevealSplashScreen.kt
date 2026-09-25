package com.example.githubissuefinder.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Stage boundaries, in ms, carried over verbatim from the reference web
// animation's own 0/28/38/50/62/100% breakpoints against its 3200ms
// timeline — kept as exact-feeling numbers rather than rounded ones so
// this stays directly comparable against that source if it's revisited.
private const val GIT_DURATION_MS = 896
private const val HUB_START_MS = GIT_DURATION_MS
private const val HUB_DURATION_MS = 320
private const val ISSUE_START_MS = HUB_START_MS + HUB_DURATION_MS
private const val ISSUE_DURATION_MS = 384
private const val FINDER_START_MS = ISSUE_START_MS + ISSUE_DURATION_MS
private const val FINDER_DURATION_MS = 384
private const val HOLD_MS = 900

private val OFFSCREEN_OFFSET = 260.dp
private val VERTICAL_RISE_OFFSET = 140.dp
private const val GIT_OVERSHOOT_PX = 28f
private const val GIT_OVERSHOOT_TILT_DEGREES = -4.5f

/**
 * A kinetic title-reveal splash: "Git" slides in from the left and
 * overshoots into a lurch before settling ("Screeching Stop"), "Hub"
 * slides in from the right and shudders on impact, then "Issue" and
 * "Finder" rise straight up from below into place under "Git" and "Hub"
 * respectively — spelling out the app's own name, not GitHub's (see
 * SplashBrandMark.kt's doc comment on why this project uses its own
 * brand colors rather than GitHub's).
 *
 * Selectable via BuildConfig.SPLASH_ANIMATION, alongside "coin",
 * "origami", and "shapeStack" — see MainActivity.kt.
 *
 * Choreographed from a real reference animation Gene captured frame-by-
 * frame from a separate web prototype (a kinetic-typography player with
 * its own named stages and an explicit "+28px, -4.5° tilt" spec for the
 * "Git" overshoot, which is where those two constants above come from
 * directly). This is a first pass built from that frame sequence and
 * stage spec alone, deliberately simpler than the source in one
 * respect: "Issue" and "Finder" here rise straight to their final size
 * and position, rather than first appearing as an oversized, near-
 * transparent "ghost" that then shrinks into place the way the
 * reference does for every incoming word. That ghost-then-shrink layer
 * is a reasonable follow-up once this simpler version is confirmed
 * working, not something this pass attempts.
 *
 * Each word gets its own Animatable(s) launched in a separate coroutine
 * with a `delay()` to its stage's start time, rather than one shared
 * global progress value — keeps each word's motion independently
 * readable, and matches the reference's own per-word stage structure
 * more directly than deriving everything from a single 0..1 fraction
 * would.
 */
@Composable
fun TitleRevealSplashScreen(onFinished: () -> Unit) {
    val density = LocalDensity.current

    val gitOffsetX = remember { Animatable(0f) }
    val gitRotation = remember { Animatable(0f) }
    val hubOffsetX = remember { Animatable(0f) }
    val issueOffsetY = remember { Animatable(0f) }
    val issueAlpha = remember { Animatable(0f) }
    val finderOffsetY = remember { Animatable(0f) }
    val finderAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        val offscreenPx = with(density) { OFFSCREEN_OFFSET.toPx() }
        val riseOffsetPx = with(density) { VERTICAL_RISE_OFFSET.toPx() }

        // "Git": screech in from off-left, overshoot forward past its
        // resting spot, then ease back — the lurch-forward-then-back
        // Gene specifically called out, quantified by the reference's
        // own "+28px, -4.5° tilt, simulating high-inertia braking" spec.
        launch {
            gitOffsetX.snapTo(-offscreenPx)
            gitOffsetX.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = GIT_DURATION_MS
                    -offscreenPx at 0
                    GIT_OVERSHOOT_PX at (GIT_DURATION_MS * 0.75).toInt() using FastOutSlowInEasing
                    0f at GIT_DURATION_MS using FastOutSlowInEasing
                },
            )
        }
        launch {
            gitRotation.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = GIT_DURATION_MS
                    0f at 0
                    GIT_OVERSHOOT_TILT_DEGREES at (GIT_DURATION_MS * 0.75).toInt() using FastOutSlowInEasing
                    0f at GIT_DURATION_MS using FastOutSlowInEasing
                },
            )
        }

        // "Hub": slides in from off-right, then a quick decaying
        // shudder on collision with "Git" before settling flush.
        launch {
            delay(HUB_START_MS.toLong())
            hubOffsetX.snapTo(offscreenPx)
            hubOffsetX.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = HUB_DURATION_MS
                    offscreenPx at 0
                    0f at (HUB_DURATION_MS * 0.55).toInt() using FastOutSlowInEasing
                    with(density) { 10.dp.toPx() } at (HUB_DURATION_MS * 0.7).toInt()
                    with(density) { (-6).dp.toPx() } at (HUB_DURATION_MS * 0.85).toInt()
                    0f at HUB_DURATION_MS
                },
            )
        }

        // "Issue": a straight rise from below into place under "Git" —
        // simpler than the reference's ghost-then-shrink treatment, see
        // this file's doc comment.
        launch {
            delay(ISSUE_START_MS.toLong())
            issueOffsetY.snapTo(riseOffsetPx)
            launch {
                issueOffsetY.animateTo(0f, tween(ISSUE_DURATION_MS, easing = FastOutSlowInEasing))
            }
            issueAlpha.animateTo(1f, tween(ISSUE_DURATION_MS))
        }

        // "Finder": the same straight rise, under "Hub".
        launch {
            delay(FINDER_START_MS.toLong())
            finderOffsetY.snapTo(riseOffsetPx)
            launch {
                finderOffsetY.animateTo(0f, tween(FINDER_DURATION_MS, easing = FastOutSlowInEasing))
            }
            finderAlpha.animateTo(1f, tween(FINDER_DURATION_MS))
        }

        delay((FINDER_START_MS + FINDER_DURATION_MS + HOLD_MS).toLong())
        onFinished()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row {
                Text(
                    "Git",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.graphicsLayer {
                        translationX = gitOffsetX.value
                        rotationZ = gitRotation.value
                    },
                )
                Text(
                    "Hub",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.graphicsLayer { translationX = hubOffsetX.value },
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Text(
                    "ISSUE",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 16.sp,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.graphicsLayer {
                        translationY = issueOffsetY.value
                        alpha = issueAlpha.value
                    },
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Finder",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.graphicsLayer {
                        translationY = finderOffsetY.value
                        alpha = finderAlpha.value
                    },
                )
            }
        }
    }
}
