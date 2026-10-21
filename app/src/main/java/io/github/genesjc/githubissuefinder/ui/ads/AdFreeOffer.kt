package io.github.genesjc.githubissuefinder.ui.ads

import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.genesjc.githubissuefinder.R
import kotlinx.coroutines.delay

private const val MILLIS_PER_MINUTE = 60_000L

/** How often the countdown is redrawn while an ad-free period runs. */
private const val COUNTDOWN_TICK_MS = 15_000L

/** Whole minutes left until [untilMs], rounded up so the last partial minute still reads "1 min"; 0 when over. */
fun adFreeMinutesLeft(untilMs: Long, nowMs: Long): Int {
    val remainingMs = untilMs - nowMs
    if (remainingMs <= 0) return 0
    return ((remainingMs + MILLIS_PER_MINUTE - 1) / MILLIS_PER_MINUTE).toInt()
}

/**
 * The small line in the title bar for the ad-free period: an offer to hide ads when one can be made, the
 * time left while a period runs, and nothing otherwise (always nothing in the open build). It never asks
 * anyone to look at or tap an ad; the wording is only about getting time without ads.
 */
@Composable
fun AdFreeOfferLine(
    adFreeUntilMs: Long,
    canOffer: Boolean,
    onOffer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nowMs = rememberNowMillis(adFreeUntilMs)
    val minutesLeft = adFreeMinutesLeft(adFreeUntilMs, nowMs)
    when {
        minutesLeft > 0 -> Text(
            stringResource(R.string.ad_free_remaining, minutesLeft),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
        canOffer -> Text(
            stringResource(R.string.action_hide_ads),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = modifier.clickable(onClick = onOffer),
        )
    }
}

/** The current time, redrawn every few seconds while [untilMs] is still ahead, and still afterwards. */
@Composable
private fun rememberNowMillis(untilMs: Long): Long {
    var nowMs by remember(untilMs) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(untilMs) {
        while (nowMs < untilMs) {
            delay(COUNTDOWN_TICK_MS)
            nowMs = System.currentTimeMillis()
        }
    }
    return nowMs
}
