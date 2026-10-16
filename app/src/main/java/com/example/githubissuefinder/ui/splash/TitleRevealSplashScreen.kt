package com.example.githubissuefinder.ui.splash

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
private const val GIT_OVERSHOOT_PX = 28f
private const val GIT_OVERSHOOT_TILT_DEGREES = -4.5f

// Scaled with the text (see SPLASH_TEXT_SCALE) so the burst keeps its
// proportions relative to the bigger wordmark.
private val BURST_SIZE = (120 * SPLASH_TEXT_SCALE).dp
private const val BURST_START_SCALE = 0.4f
private const val BURST_MAX_SCALE = 2.8f
private const val BURST_START_ALPHA = 0.85f
private const val BURST_HOLD_FRACTION = 0.45

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
 * directly). That same reference player has its own 0.25x/0.5x/1x/1.5x/
 * 2x playback-speed control — [speedMultiplier] mirrors that exact
 * convention rather than inventing a different one: actual stage
 * durations are each BASE_MS / speedMultiplier, so 0.5x means every
 * duration doubles (half playback speed, i.e. twice as slow), 1x (the
 * default) reproduces the reference's own numbers exactly. One
 * composable, not a duplicated "slow variant" screen — see
 * TopReposScreen.kt's replay menu for where 0.5x is offered alongside
 * the standard 1x.
 *
 * An earlier version also showed oversized, barely-visible "ghost"
 * copies of "Issue" and "Finder" from the first frame, fading out as the
 * real words arrived; removed because the faint imprint of upcoming
 * text didn't read well. The text itself is drawn by TitleWordmark,
 * shared with the short fade-in splash (TitleFlashSplashScreen) and
 * scaled up 70% (SPLASH_TEXT_SCALE) so it fills more of the screen.
 *
 * Each word gets its own Animatable(s) launched in a separate coroutine
 * with a `delay()` to its stage's start time, rather than one shared
 * global progress value — keeps each word's motion independently
 * readable, and matches the reference's own per-word stage structure
 * more directly than deriving everything from a single 0..1 fraction
 * would.
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
    // lands — shared between the shudder keyframes below and the impact
    // burst, so the burst can never drift out of sync with the hit it's
    // marking, at any speed.
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
    val gitOffsetX = remember { Animatable(-offscreenPx) }
    val gitRotation = remember { Animatable(0f) }
    val hubOffsetX = remember { Animatable(offscreenPx) }
    val issueOffsetY = remember { Animatable(riseOffsetPx) }
    val issueAlpha = remember { Animatable(0f) }
    val finderOffsetY = remember { Animatable(riseOffsetPx) }
    val finderAlpha = remember { Animatable(0f) }
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
                    GIT_OVERSHOOT_PX at (gitDurationMs * 0.75).toInt() using FastOutSlowInEasing
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
                    GIT_OVERSHOOT_TILT_DEGREES at (gitDurationMs * 0.75).toInt() using FastOutSlowInEasing
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

        // The impact burst: a soft radial pulse behind the wordmark,
        // right at the instant "Hub" actually lands (hubImpactOffsetMs
        // into Hub's own entrance, same moment its shudder keyframe
        // uses) — not the reference's literal effect, just a cheap
        // radial-gradient stand-in for the glow visible in Gene's
        // captured frames at that same collision moment.
        //
        // Alpha deliberately HOLDS near its starting value for the first
        // BURST_HOLD_FRACTION of the duration, rather than fading on the
        // same default easing driving the scale-up — both moving fast
        // right from the start (as plain default-eased tweens) meant the
        // circle was already nearly transparent by the time it grew
        // large enough to actually notice, so in practice nothing
        // visible ever showed up. Holding alpha up while the scale grows,
        // THEN fading, is what actually makes the burst readable.
        launch {
            delay((hubStartMs + hubImpactOffsetMs).toLong())
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
