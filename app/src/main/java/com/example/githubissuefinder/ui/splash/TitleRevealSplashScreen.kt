package com.example.githubissuefinder.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.githubissuefinder.model.IssueLabelFilter
import com.example.githubissuefinder.ui.theme.colorScheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Stage durations, in ms, carried over verbatim from the reference web
// animation's own 0/28/38/50/62/100% breakpoints against its 3200ms
// timeline — kept as exact-feeling numbers rather than rounded ones so
// this stays directly comparable against that source if it's revisited.
// These are the BASE (1x) durations; TitleRevealSplashScreen's
// speedMultiplier param scales them at call time — see that doc comment.
private const val GIT_BASE_DURATION_MS = 896
private const val HUB_BASE_DURATION_MS = 320
private const val ISSUE_BASE_DURATION_MS = 384
private const val FINDER_BASE_DURATION_MS = 384
private const val HOLD_BASE_MS = 900
private const val BURST_BASE_DURATION_MS = 500

private val OFFSCREEN_OFFSET = 260.dp
private val VERTICAL_RISE_OFFSET = 140.dp
// How hard "Git" lurches when it stops. The reference prototype specified
// "+28px, -4.5deg"; those were CSS pixels (roughly dp), but this was first
// ported as 28 raw PIXELS — about 9dp on a typical 3x phone, which read as
// barely there. Now in dp, and deliberately pushed well past the reference
// (a bigger lurch, a harder tilt, and a small rebound back) so the stop is
// an unmistakable bump.
private val GIT_OVERSHOOT = 44.dp
private val GIT_REBOUND = 10.dp
private const val GIT_OVERSHOOT_TILT_DEGREES = -8f
private const val GIT_REBOUND_TILT_DEGREES = 2.5f
private const val GIT_OVERSHOOT_AT = 0.70
private const val GIT_REBOUND_AT = 0.86

/**
 * A kinetic title-reveal splash: "Git" slides in from the left and overshoots before settling, "Hub"
 * slides in from the right and shudders on impact, then "Issue" and "Finder" rise into place beneath
 * them, spelling the app's own name (see SplashBrandMark.kt on why it does not use GitHub's brand).
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
    val gitDurationMs = (GIT_BASE_DURATION_MS / speedMultiplier).toInt()
    val hubStartMs = gitDurationMs
    val hubDurationMs = (HUB_BASE_DURATION_MS / speedMultiplier).toInt()
    val issueStartMs = hubStartMs + hubDurationMs
    val issueDurationMs = (ISSUE_BASE_DURATION_MS / speedMultiplier).toInt()
    val finderStartMs = issueStartMs + issueDurationMs
    val finderDurationMs = (FINDER_BASE_DURATION_MS / speedMultiplier).toInt()
    val holdMs = (HOLD_BASE_MS / speedMultiplier).toInt()
    val burstDurationMs = (BURST_BASE_DURATION_MS / speedMultiplier).toInt()
    // Exactly when, within Hub's own entrance, the collision itself
    // lands — the moment its shudder keyframes below start settling.
    val hubImpactOffsetMs = (hubDurationMs * 0.55).toInt()

    // Every animated value STARTS where its animation begins — "Git" off
    // to the left, "Hub" off to the right, the rising words below — not
    // at its final resting position. They used to start at the resting
    // position (0) and only be moved off-screen by a snapTo() inside the
    // LaunchedEffect below, which runs after the first frame has already
    // been drawn; on a slow cold start (a fresh install) that first frame
    // could sit on screen for a noticeable time, showing the finished
    // title, before it jumped to blank and the animation began.
    val offscreenPx = with(density) { OFFSCREEN_OFFSET.toPx() }
    val riseOffsetPx = with(density) { VERTICAL_RISE_OFFSET.toPx() }
    val gitOvershootPx = with(density) { GIT_OVERSHOOT.toPx() }
    val gitReboundPx = with(density) { GIT_REBOUND.toPx() }
    val gitOffsetX = remember { Animatable(-offscreenPx) }
    val gitRotation = remember { Animatable(0f) }
    val hubOffsetX = remember { Animatable(offscreenPx) }
    val issueOffsetY = remember { Animatable(riseOffsetPx) }
    val issueAlpha = remember { Animatable(0f) }
    val finderOffsetY = remember { Animatable(riseOffsetPx) }
    val finderAlpha = remember { Animatable(0f) }
    val burst = remember { BurstAnimation() }

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

        // "Git": screech in from off-left, overshoot forward past its
        // resting spot, then ease back — the lurch-forward-then-back
        // Gene specifically called out, quantified by the reference's
        // own "+28px, -4.5° tilt, simulating high-inertia braking" spec.
        launch {
            gitOffsetX.snapTo(-offscreenPx)
            gitOffsetX.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = gitDurationMs
                    -offscreenPx at 0
                    gitOvershootPx at (gitDurationMs * GIT_OVERSHOOT_AT).toInt() using FastOutSlowInEasing
                    -gitReboundPx at (gitDurationMs * GIT_REBOUND_AT).toInt() using FastOutSlowInEasing
                    0f at gitDurationMs using FastOutSlowInEasing
                },
            )
        }
        launch {
            gitRotation.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = gitDurationMs
                    0f at 0
                    GIT_OVERSHOOT_TILT_DEGREES at (gitDurationMs * GIT_OVERSHOOT_AT).toInt() using FastOutSlowInEasing
                    GIT_REBOUND_TILT_DEGREES at (gitDurationMs * GIT_REBOUND_AT).toInt() using FastOutSlowInEasing
                    0f at gitDurationMs using FastOutSlowInEasing
                },
            )
        }

        // "Hub": slides in from off-right, then a quick decaying
        // shudder on collision with "Git" before settling flush.
        launch {
            // Off-screen from the very first frame, same as "Git"'s own
            // immediate snapTo above — snapping AFTER the delay instead
            // (the original bug here) left Hub sitting fully visible at
            // its final resting spot for the entire wait, since nothing
            // had moved it yet.
            hubOffsetX.snapTo(offscreenPx)
            delay(hubStartMs.toLong())
            hubOffsetX.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = hubDurationMs
                    offscreenPx at 0
                    0f at hubImpactOffsetMs using FastOutSlowInEasing
                    with(density) { 10.dp.toPx() } at (hubDurationMs * 0.7).toInt()
                    with(density) { (-6).dp.toPx() } at (hubDurationMs * 0.85).toInt()
                    0f at hubDurationMs
                },
            )
        }

        // The finishing burst, fired once everything has settled.
        launch {
            delay((finderStartMs + finderDurationMs).toLong())
            burst.play(burstDurationMs)
        }

        // "Issue": a straight rise from below into place under "Git",
        // fading in as it goes.
        launch {
            delay(issueStartMs.toLong())
            issueOffsetY.snapTo(riseOffsetPx)
            launch {
                issueOffsetY.animateTo(0f, tween(issueDurationMs, easing = FastOutSlowInEasing))
            }
            issueAlpha.animateTo(1f, tween(issueDurationMs))
        }

        // "Finder": the same straight rise and fade-in, under "Hub".
        launch {
            delay(finderStartMs.toLong())
            finderOffsetY.snapTo(riseOffsetPx)
            launch {
                finderOffsetY.animateTo(0f, tween(finderDurationMs, easing = FastOutSlowInEasing))
            }
            finderAlpha.animateTo(1f, tween(finderDurationMs))
        }

        delay((finderStartMs + finderDurationMs + holdMs).toLong())
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
        // Drawn first so it sits behind the wordmark below.
        SplashBurst(burst)

        TitleWordmark(
            modifier = Modifier.graphicsLayer { alpha = if (started) 1f else 0f },
            gitModifier = Modifier.graphicsLayer {
                translationX = gitOffsetX.value
                rotationZ = gitRotation.value
            },
            hubModifier = Modifier.graphicsLayer { translationX = hubOffsetX.value },
            issueModifier = Modifier.graphicsLayer {
                translationY = issueOffsetY.value
                alpha = issueAlpha.value
            },
            finderModifier = Modifier.graphicsLayer {
                translationY = finderOffsetY.value
                alpha = finderAlpha.value
            },
        )
    }
    }
}
