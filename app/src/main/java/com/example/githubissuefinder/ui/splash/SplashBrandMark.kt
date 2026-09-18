package com.example.githubissuefinder.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shared "GH" brand mark used by every splash animation variant (coin
 * flip and origami unfold so far, selectable via
 * BuildConfig.SPLASH_ANIMATION) — so switching which animation plays
 * never changes what the resting mark itself looks like, only how it
 * arrives on screen.
 *
 * Deliberately not GitHub's actual logo — see SplashScreen.kt's original
 * doc comment for the full reasoning (independent, unaffiliated
 * portfolio project).
 */
@Composable
fun SplashBrandMark(modifier: Modifier = Modifier, size: Dp = 96.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "GH",
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}
