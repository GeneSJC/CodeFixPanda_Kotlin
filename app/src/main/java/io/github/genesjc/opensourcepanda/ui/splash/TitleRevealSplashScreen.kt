package io.github.genesjc.opensourcepanda.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.genesjc.opensourcepanda.model.IssueLabelFilter
import io.github.genesjc.opensourcepanda.ui.theme.colorScheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Stage durations, in ms, carried over verbatim from the reference web
// animation's own 0/28/38/50/62/100% breakpoints against its 3200ms
// timeline — kept as exact-feeling numbers rather than rounded ones so
// this stays directly comparable against that source if it's revisited.
// These are the BASE (1x) durations; TitleRevealSplashScreen's
// speedMultiplier param scales them at call time — see that doc comment.
private const val OPEN_BASE_DURATION_MS = 896
private const val SOURCE_BASE_DURATION_MS = 320
private const val PANDA_BASE_DURATION_MS = 384
private const val HOLD_BASE_MS = 900
private const val BURST_BASE_DURATION_MS = 500

private val OFFSCREEN_OFFSET = 260.dp
private val VERTICAL_RISE_OFFSET = 140.dp
// How hard "Open" lurches when it stops. The reference prototype specified
// "+28px, -4.5deg"; those were CSS pixels (roughly dp), but this was first
// ported as 28 raw PIXELS — about 9dp on a typical 3x phone, which read as
// barely there. Now in dp, and deliberately pushed well past the reference
// (a bigger lurch, a harder tilt, and a small rebound back) so the stop is
// an unmistakable bump.
private val OPEN_OVERSHOOT = 44.dp
private val OPEN_REBOUND = 10.dp
private const val OPEN_OVERSHOOT_TILT_DEGREES = -8f
private const val OPEN_REBOUND_TILT_DEGREES = 2.5f
private const val OPEN_OVERSHOOT_AT = 0.70
private const val OPEN_REBOUND_AT = 0.86

// Scaled with the text (see SPLASH_TEXT_SCALE) so the burst keeps its
// proportions relative to the bigger wordmark.
private val BURST_SIZE = (120 * SPLASH_TEXT_SCALE).dp
private const val BURST_START_SCALE = 0.4f
private const val BURST_MAX_SCALE = 2.8f
private const val BURST_START_ALPHA = 0.85f
private const val BURST_HOLD_FRACTION = 0.45

/**
 * A kinetic title-reveal splash: "Open" slides in from the left and overshoots before settling, "Source"
 * slides in from the right and shudders on impact, then "Panda" rises into place beneath them,
 * spelling the app's own name (see SplashBrandMark.kt on why it does not use GitSource's brand).
 * [speedMultiplier] scales every stage duration (0.5 means twice as slow), so one composable serves
 * the standard and half-speed replays. Each word has its own Animatable(s), launched in a coroutine
 * with a delay to its stage start, which keeps each word's motion readable. The text is drawn by
 * TitleWordmark, shared with the short fade-in splash.
 */
@Composable
fun TitleRevealSplashScreen(onFinished: () -> Unit, speedMultiplier: Float = 1f) {
    val density = LocalDensity.current

    // Actual, speed-scaled stage timing for this call — see this
    // function's doc comment for why division (not multiplication) by
    // speedMultiplier is the right direction: 0.5x should mean twice as
    // long, matching a video player's own "0.5x speed" control.
    val openDurationMs = (OPEN_BASE_DURATION_MS / speedMultiplier).toInt()
    val sourceStartMs = openDurationMs
    val sourceDurationMs = (SOURCE_BASE_DURATION_MS / speedMultiplier).toInt()
    val pandaStartMs = sourceStartMs + sourceDurationMs
    val pandaDurationMs = (PANDA_BASE_DURATION_MS / speedMultiplier).toInt()
    val holdMs = (HOLD_BASE_MS / speedMultiplier).toInt()
    val burstDurationMs = (BURST_BASE_DURATION_MS / speedMultiplier).toInt()
    // Exactly when, within Source's own entrance, the collision itself
    // lands — the moment its shudder keyframes below start settling.
    val sourceImpactOffsetMs = (sourceDurationMs * 0.55).toInt()

    // Every animated value STARTS where its animation begins — "Open" off
    // to the left, "Source" off to the right, the rising words below — not
    // at its final resting position. They used to start at the resting
    // position (0) and only be moved off-screen by a snapTo() inside the
    // LaunchedEffect below, which runs after the first frame has already
    // been drawn; on a slow cold start (a fresh install) that first frame
    // could sit on screen for a noticeable time, showing the finished
    // title, before it jumped to blank and the animation began.
    val offscreenPx = with(density) { OFFSCREEN_OFFSET.toPx() }
    val riseOffsetPx = with(density) { VERTICAL_RISE_OFFSET.toPx() }
    val openOvershootPx = with(density) { OPEN_OVERSHOOT.toPx() }
    val openReboundPx = with(density) { OPEN_REBOUND.toPx() }
    val openOffsetX = remember { Animatable(-offscreenPx) }
    val openRotation = remember { Animatable(0f) }
    val sourceOffsetX = remember { Animatable(offscreenPx) }
    val pandaOffsetY = remember { Animatable(riseOffsetPx) }
    val pandaAlpha = remember { Animatable(0f) }
    val burstScale = remember { Animatable(BURST_START_SCALE) }
    val burstAlpha = remember { Animatable(0f) }

    // The whole wordmark stays invisible until the timeline is about to
    // start, as a second guard: even if some value were ever wrong for a
    // frame, nothing is drawn until `started` flips.
    var started by remember { mutableStateOf(false) }

    LaunchedEffect(speedMultiplier) {
        // Let the first couple of frames go by (drawing the blank screen)
        // BEFORE starting any timer or animation. On a cold start the
        // first frame can be very slow (class loading, first-time Compose
        // set-up); starting the timeline inside that hitch would make the
        // opening of the animation stutter. After these two frames the
        // screen is up and the timeline starts smoothly.
        withFrameNanos { }
        withFrameNanos { }
        started = true

        // "Open": screech in from off-left, overshoot forward past its
        // resting spot, then ease back — the lurch-forward-then-back
        // Gene specifically called out, quantified by the reference's
        // own "+28px, -4.5° tilt, simulating high-inertia braking" spec.
        launch {
            openOffsetX.snapTo(-offscreenPx)
            openOffsetX.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = openDurationMs
                    -offscreenPx at 0
                    openOvershootPx at (openDurationMs * OPEN_OVERSHOOT_AT).toInt() using FastOutSlowInEasing
                    -openReboundPx at (openDurationMs * OPEN_REBOUND_AT).toInt() using FastOutSlowInEasing
                    0f at openDurationMs using FastOutSlowInEasing
                },
            )
        }
        launch {
            openRotation.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = openDurationMs
                    0f at 0
                    OPEN_OVERSHOOT_TILT_DEGREES at (openDurationMs * OPEN_OVERSHOOT_AT).toInt() using FastOutSlowInEasing
                    OPEN_REBOUND_TILT_DEGREES at (openDurationMs * OPEN_REBOUND_AT).toInt() using FastOutSlowInEasing
                    0f at openDurationMs using FastOutSlowInEasing
                },
            )
        }

        // "Source": slides in from off-right, then a quick decaying
        // shudder on collision with "Open" before settling flush.
        launch {
            // Off-screen from the very first frame, same as "Open"'s own
            // immediate snapTo above — snapping AFTER the delay instead
            // (the original bug here) left Source sitting fully visible at
            // its final resting spot for the entire wait, since nothing
            // had moved it yet.
            sourceOffsetX.snapTo(offscreenPx)
            delay(sourceStartMs.toLong())
            sourceOffsetX.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = sourceDurationMs
                    offscreenPx at 0
                    0f at sourceImpactOffsetMs using FastOutSlowInEasing
                    with(density) { 10.dp.toPx() } at (sourceDurationMs * 0.7).toInt()
                    with(density) { (-6).dp.toPx() } at (sourceDurationMs * 0.85).toInt()
                    0f at sourceDurationMs
                },
            )
        }

        // The finishing burst: a soft radial pulse behind the wordmark, fired once everything has settled.
        // Alpha holds near its start for the first BURST_HOLD_FRACTION before fading; fading and scaling from
        // the start left the circle nearly transparent by the time it was big enough to see.
        launch {
            delay((pandaStartMs + pandaDurationMs).toLong())
            launch {
                burstScale.animateTo(BURST_MAX_SCALE, tween(burstDurationMs, easing = FastOutSlowInEasing))
            }
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

        // "Panda": a straight rise from below into place under
        // "Open Source", fading in as it goes.
        launch {
            delay(pandaStartMs.toLong())
            pandaOffsetY.snapTo(riseOffsetPx)
            launch {
                pandaOffsetY.animateTo(0f, tween(pandaDurationMs, easing = FastOutSlowInEasing))
            }
            pandaAlpha.animateTo(1f, tween(pandaDurationMs))
        }

        delay((pandaStartMs + pandaDurationMs + holdMs).toLong())
        onFinished()
    }

    // Pinned to the Good First Issue (purple) palette regardless of the
    // app's currently active Issue Mode — the splash is this project's
    // own fixed brand identity (see SplashBrandMark's doc comment), not
    // something that should repaint itself green under Help Wanted just
    // because Theme.kt's root MaterialTheme call happens to be mode-
    // aware. Without this override, MaterialTheme.colorScheme reads
    // straight from that ambient, mode-driven scheme, so replaying this
    // animation (or a real cold start with Help Wanted already chosen)
    // would show it in green instead of the intended purple.
    MaterialTheme(colorScheme = IssueLabelFilter.GOOD_FIRST_ISSUE.colorScheme()) {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        // Drawn first so it sits BEHIND the wordmark below (draw order
        // in Compose follows declaration order within a Box).
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

        TitleWordmark(
            modifier = Modifier.graphicsLayer { alpha = if (started) 1f else 0f },
            openModifier = Modifier.graphicsLayer {
                translationX = openOffsetX.value
                rotationZ = openRotation.value
            },
            sourceModifier = Modifier.graphicsLayer { translationX = sourceOffsetX.value },
            pandaModifier = Modifier.graphicsLayer {
                translationY = pandaOffsetY.value
                alpha = pandaAlpha.value
            },
        )
    }
    }
}
