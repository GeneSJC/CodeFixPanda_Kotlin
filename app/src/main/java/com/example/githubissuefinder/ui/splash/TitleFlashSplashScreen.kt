package com.example.githubissuefinder.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.example.githubissuefinder.model.IssueLabelFilter
import com.example.githubissuefinder.ui.theme.colorScheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val FLASH_FADE_IN_MS = 300
private const val FLASH_HOLD_MS = 1200L
private const val FLASH_BURST_MS = 1200

// How long before the burst ends the app starts to appear underneath while the splash fades out.
private const val FLASH_HANDOFF_MS = 350

/**
 * The short splash for every launch after the first: the app name, already in its final position
 * (TitleWordmark), fades in and holds on its own for about a second and a half. Then the same
 * "sonic boom" burst as the full animation grows behind it until it covers the screen. Shortly
 * before the burst ends, [onHandoff] lets the app start composing underneath while the splash
 * fades out, so the burst flows into the app; [onFinished] follows when the fade is done. Pinned to
 * the Good First Issue (purple) palette like TitleRevealSplashScreen, because the splash is the
 * project's fixed brand identity and should not repaint green under Help Wanted.
 */
@Composable
fun TitleFlashSplashScreen(onFinished: () -> Unit, onHandoff: () -> Unit = {}) {
    val fade = remember { Animatable(0f) }
    val splashAlpha = remember { Animatable(1f) }
    val burst = remember { BurstAnimation() }
    val coverScale = rememberCoverScale()

    LaunchedEffect(Unit) {
        fade.animateTo(1f, tween(FLASH_FADE_IN_MS))
        delay(FLASH_HOLD_MS)
        coroutineScope {
            launch {
                burst.play(
                    FLASH_BURST_MS,
                    fadeOut = FastOutLinearInEasing,
                    maxScale = coverScale,
                    grow = LinearOutSlowInEasing,
                )
            }
            launch {
                delay((FLASH_BURST_MS - FLASH_HANDOFF_MS).toLong())
                onHandoff()
                splashAlpha.animateTo(0f, tween(FLASH_HANDOFF_MS))
            }
        }
        onFinished()
    }

    MaterialTheme(colorScheme = IssueLabelFilter.GOOD_FIRST_ISSUE.colorScheme()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = splashAlpha.value }
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            SplashBurst(burst)
            TitleWordmark(modifier = Modifier.graphicsLayer { alpha = fade.value })
        }
    }
}
