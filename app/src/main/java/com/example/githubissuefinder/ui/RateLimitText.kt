package com.example.githubissuefinder.ui

import androidx.compose.runtime.Composable
import com.example.githubissuefinder.R

/** "45s" or "2m 5s" for the seconds until the rate limit resets, or "unknown" when GitHub did not say. */
fun StringResolver.resetsIn(seconds: Long?): String = when {
    seconds == null -> invoke(R.string.resets_in_unknown)
    seconds < 60 -> invoke(R.string.resets_in_seconds, seconds)
    else -> invoke(R.string.resets_in_minutes_seconds, seconds / 60, seconds % 60)
}

/** [resetsIn] for a composable. */
@Composable
fun resetsInText(seconds: Long?): String = rememberStringResolver().resetsIn(seconds)
