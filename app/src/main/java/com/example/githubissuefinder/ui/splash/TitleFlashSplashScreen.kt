package com.example.githubissuefinder.ui.splash

import androidx.compose.animation.core.Animatable
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
import kotlinx.coroutines.delay

private const val FLASH_FADE_IN_MS = 300
private const val FLASH_HOLD_MS = 1200L

/**
 * The short splash shown on every launch after the first: the app name,
 * already centered in its final position (TitleWordmark — the same
 * layout as the full animation's last frame), fading in and holding for
 * about a second and a half total before handing off to the app. No
 * motion, no collision burst — the full title-reveal is fun once and a
 * bit much on repeat viewings.
 *
 * Pinned to the Good First Issue (purple) palette for the same reason
 * TitleRevealSplashScreen is: the splash is the project's fixed brand
 * identity, not something that should repaint itself green under Help
 * Wanted just because the root theme is mode-aware.
 */
@Composable
fun TitleFlashSplashScreen(onFinished: () -> Unit) {
    val fade = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        fade.animateTo(1f, tween(FLASH_FADE_IN_MS))
        delay(FLASH_HOLD_MS)
        onFinished()
    }

    MaterialTheme(colorScheme = IssueLabelFilter.GOOD_FIRST_ISSUE.colorScheme()) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            TitleWordmark(modifier = Modifier.graphicsLayer { alpha = fade.value })
        }
    }
}
