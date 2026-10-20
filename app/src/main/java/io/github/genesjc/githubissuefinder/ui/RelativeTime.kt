package io.github.genesjc.githubissuefinder.ui

import androidx.compose.runtime.Composable
import io.github.genesjc.githubissuefinder.R

private const val MILLIS_PER_HOUR = 1000 * 60 * 60

/**
 * "just now", "3h ago" or "2d ago" for a time in the past, or "recently" when the time is unknown.
 * The data layer keeps the timestamp; this is the wording, so it can come from string resources.
 * [nowMillis] is a parameter so the buckets can be tested.
 */
fun StringResolver.relativeTime(thenMillis: Long?, nowMillis: Long = System.currentTimeMillis()): String {
    if (thenMillis == null) return invoke(R.string.time_recently)
    val hoursAgo = (nowMillis - thenMillis) / MILLIS_PER_HOUR
    return when {
        hoursAgo < 1 -> invoke(R.string.time_just_now)
        hoursAgo < 24 -> invoke(R.string.time_hours_ago, hoursAgo)
        else -> invoke(R.string.time_days_ago, hoursAgo / 24)
    }
}

/** [relativeTime] for a composable, which reads the clock each time it is drawn. */
@Composable
fun relativeTimeText(thenMillis: Long?): String = rememberStringResolver().relativeTime(thenMillis)
