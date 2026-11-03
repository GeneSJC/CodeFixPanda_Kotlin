package io.github.genesjc.opensourcepanda.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Stage durations, in ms, carried over verbatim from the reference web
// animation's own 0/28/38/50/62/100% breakpoints against its 3200ms
// timeline — kept as exact-feeling numbers rather than rounded ones so
// this stays directly comparable against that source if it's revisited.
// These are the BASE (1x) durations; TitleRevealTimeline's
// speedMultiplier param scales them when it is built — see its doc comment.
private const val OPEN_BASE_DURATION_MS = 896
private const val SOURCE_BASE_DURATION_MS = 320
private const val PANDA_BASE_DURATION_MS = 384
private const val HOLD_BASE_MS = 900
private const val BURST_BASE_DURATION_MS = 500

// The panda drops in after "PANDA" has risen, falls with gravity (an accelerating curve), and lands with a
// squash from its base; the burst fires at the moment it lands.
private const val MASCOT_DROP_BASE_MS = 420
private const val MASCOT_SQUASH_BASE_MS = 300
private val MASCOT_DROP_OFFSET = 200.dp
private const val MASCOT_SQUASH_Y = 0.88f
private const val MASCOT_SQUASH_X = 1.10f
private const val MASCOT_REBOUND_Y = 1.04f
private const val MASCOT_REBOUND_X = 0.98f

private val OFFSCREEN_OFFSET = 260.dp
private val VERTICAL_RISE_OFFSET = 140.dp
// How hard "Open" lurches when it stops: well past the reference prototype's +28dp / -4.5deg
// (a bigger lurch, a harder tilt, a small rebound back) so the stop is an unmistakable bump.
private val OPEN_OVERSHOOT = 44.dp
private val OPEN_REBOUND = 10.dp
private const val OPEN_OVERSHOOT_TILT_DEGREES = -8f
private const val OPEN_REBOUND_TILT_DEGREES = 2.5f
private const val OPEN_OVERSHOOT_AT = 0.70
private const val OPEN_REBOUND_AT = 0.86

private const val BURST_START_SCALE = 0.4f
private const val BURST_MAX_SCALE = 2.8f
private const val BURST_START_ALPHA = 0.85f
private const val BURST_HOLD_FRACTION = 0.45

/**
 * The timeline of the title-reveal splash: each stage's start and length, the animated values the wordmark
 * reads (offsets, tilt, alpha, scale), and [play], which runs the stages. TitleRevealSplashScreen only draws
 * what these values say. [speedMultiplier] scales every stage duration (0.5 means twice as slow).
 */
internal class TitleRevealTimeline(private val density: Density, speedMultiplier: Float) {

    // Actual, speed-scaled stage timing for this run: division (not multiplication) by speedMultiplier is the
    // right direction, since 0.5x should mean twice as long, matching a video player's own "0.5x speed" control.
    private val openDurationMs = (OPEN_BASE_DURATION_MS / speedMultiplier).toInt()
    private val sourceStartMs = openDurationMs
    private val sourceDurationMs = (SOURCE_BASE_DURATION_MS / speedMultiplier).toInt()
    private val pandaStartMs = sourceStartMs + sourceDurationMs
    private val pandaDurationMs = (PANDA_BASE_DURATION_MS / speedMultiplier).toInt()
    private val mascotStartMs = pandaStartMs + pandaDurationMs
    private val mascotDropMs = (MASCOT_DROP_BASE_MS / speedMultiplier).toInt()
    private val mascotSquashMs = (MASCOT_SQUASH_BASE_MS / speedMultiplier).toInt()
    private val mascotLandMs = mascotStartMs + mascotDropMs
    private val holdMs = (HOLD_BASE_MS / speedMultiplier).toInt()
    private val burstDurationMs = (BURST_BASE_DURATION_MS / speedMultiplier).toInt()

    // Exactly when, within Source's own entrance, the collision itself lands: the moment its shudder keyframes
    // below start settling.
    private val sourceImpactOffsetMs = (sourceDurationMs * 0.55).toInt()

    // Every animated value STARTS where its animation begins: "Open" off to the left, "Source" off to the
    // right, the rising words below, not at its final resting position. They used to start at the resting
    // position (0) and only be moved off-screen by a snapTo() inside the effect that plays the timeline,
    // which runs after the first frame has already been drawn; on a slow cold start (a fresh install) that
    // first frame could sit on screen for a noticeable time, showing the finished title, before it jumped to
    // blank and the animation began.
    private val offscreenPx = with(density) { OFFSCREEN_OFFSET.toPx() }
    private val riseOffsetPx = with(density) { VERTICAL_RISE_OFFSET.toPx() }
    private val openOvershootPx = with(density) { OPEN_OVERSHOOT.toPx() }
    private val openReboundPx = with(density) { OPEN_REBOUND.toPx() }
    private val mascotDropPx = with(density) { MASCOT_DROP_OFFSET.toPx() }

    val openOffsetX = Animatable(-offscreenPx)
    val openRotation = Animatable(0f)
    val sourceOffsetX = Animatable(offscreenPx)
    val pandaOffsetY = Animatable(riseOffsetPx)
    val pandaAlpha = Animatable(0f)
    val mascotOffsetY = Animatable(-mascotDropPx)
    val mascotAlpha = Animatable(0f)
    val mascotScaleX = Animatable(1f)
    val mascotScaleY = Animatable(1f)
    val burstScale = Animatable(BURST_START_SCALE)
    val burstAlpha = Animatable(0f)

    /** Starts every stage at its own delay, waits for the last to land and the hold to pass, then calls [onFinished]. */
    suspend fun play(onFinished: () -> Unit) = coroutineScope {
        launchOpen()
        launchSource()
        launchBurst()
        launchPanda()
        launchMascot()

        delay((mascotLandMs + mascotSquashMs + holdMs).toLong())
        onFinished()
    }

    /**
     * "Open": screech in from off-left, overshoot forward past its resting spot, then ease back: the
     * lurch-forward-then-back Gene specifically called out, quantified by the reference's own "+28px, -4.5°
     * tilt, simulating high-inertia braking" spec.
     */
    private fun CoroutineScope.launchOpen() {
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
    }

    /** "Source": slides in from off-right, then a quick decaying shudder on collision with "Open" before settling flush. */
    private fun CoroutineScope.launchSource() {
        launch {
            // Off-screen from the very first frame, same as "Open"'s own immediate snapTo: snapping AFTER the
            // delay instead (the original bug here) left Source sitting fully visible at its final resting
            // spot for the entire wait, since nothing had moved it yet.
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
    }

    /**
     * The finishing burst: a soft radial pulse behind the panda, fired the moment it lands. Alpha holds near its
     * start for the first BURST_HOLD_FRACTION before fading; fading and scaling from the start left the circle
     * nearly transparent by the time it was big enough to see.
     */
    private fun CoroutineScope.launchBurst() {
        launch {
            delay(mascotLandMs.toLong())
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
    }

    /** "Panda": a straight rise from below into place under "Open Source", fading in as it goes. */
    private fun CoroutineScope.launchPanda() {
        launch {
            delay(pandaStartMs.toLong())
            pandaOffsetY.snapTo(riseOffsetPx)
            launch {
                pandaOffsetY.animateTo(0f, tween(pandaDurationMs, easing = FastOutSlowInEasing))
            }
            pandaAlpha.animateTo(1f, tween(pandaDurationMs))
        }
    }

    /** The panda: drops from above (accelerating, fading in over the first part of the fall), then squashes on landing from its base and settles with a small rebound. */
    private fun CoroutineScope.launchMascot() {
        launch {
            delay(mascotStartMs.toLong())
            launch {
                mascotAlpha.animateTo(1f, tween((mascotDropMs * 0.3f).toInt()))
            }
            mascotOffsetY.animateTo(0f, tween(mascotDropMs, easing = FastOutLinearInEasing))
            launch {
                mascotScaleY.animateTo(
                    targetValue = 1f,
                    animationSpec = keyframes {
                        durationMillis = mascotSquashMs
                        1f at 0
                        MASCOT_SQUASH_Y at (mascotSquashMs * 0.3f).toInt() using FastOutSlowInEasing
                        MASCOT_REBOUND_Y at (mascotSquashMs * 0.7f).toInt() using FastOutSlowInEasing
                        1f at mascotSquashMs using FastOutSlowInEasing
                    },
                )
            }
            mascotScaleX.animateTo(
                targetValue = 1f,
                animationSpec = keyframes {
                    durationMillis = mascotSquashMs
                    1f at 0
                    MASCOT_SQUASH_X at (mascotSquashMs * 0.3f).toInt() using FastOutSlowInEasing
                    MASCOT_REBOUND_X at (mascotSquashMs * 0.7f).toInt() using FastOutSlowInEasing
                    1f at mascotSquashMs using FastOutSlowInEasing
                },
            )
        }
    }
}
