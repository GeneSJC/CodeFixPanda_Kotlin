package com.example.githubissuefinder.data

import com.example.githubissuefinder.network.dto.RateLimitResponseDto
import okhttp3.Headers

/**
 * remaining/limit/used as reported by GitHub's Search API on its own
 * responses. Worth noting: the Search API has its own, separate, stricter
 * rate limit from GitHub's general REST API — these numbers reflect
 * search specifically, not the broader 5000/hour core API figure some
 * might expect. That's also why resetsInLabel counts in seconds/minutes,
 * not hours — the Search API's window is short (per-minute), so an
 * hour-granularity label would be misleadingly imprecise here.
 */
data class RateLimitInfo(
    val remaining: Int,
    val limit: Int,
    val used: Int,
    val resetsInLabel: String,
)

/**
 * Parses GitHub's X-RateLimit-* response headers into a RateLimitInfo, or
 * null if the essential headers aren't present. "used" is read directly
 * from X-RateLimit-Used when GitHub sends it, falling back to
 * limit-remaining if that header is ever absent.
 */
fun parseRateLimitHeaders(headers: Headers): RateLimitInfo? {
    val remaining = headers["X-RateLimit-Remaining"]?.toIntOrNull() ?: return null
    val limit = headers["X-RateLimit-Limit"]?.toIntOrNull() ?: return null
    val used = headers["X-RateLimit-Used"]?.toIntOrNull() ?: (limit - remaining)
    val resetEpochSeconds = headers["X-RateLimit-Reset"]?.toLongOrNull()
    val resetsInLabel = resetEpochSeconds?.let { formatResetsIn(it) } ?: "unknown"
    return RateLimitInfo(remaining, limit, used, resetsInLabel)
}

/**
 * The Search API rate-limit state from a GET /rate_limit response, used
 * to fill in the chip when a list was loaded from the cache and no
 * search response has supplied the numbers yet. [nowEpochSeconds] is a
 * parameter so the "resets in" label can be tested.
 */
fun RateLimitResponseDto.toSearchRateLimitInfo(
    nowEpochSeconds: Long = System.currentTimeMillis() / 1000,
): RateLimitInfo {
    val bucket = resources.search
    return RateLimitInfo(
        remaining = bucket.remaining,
        limit = bucket.limit,
        used = bucket.used,
        resetsInLabel = formatResetsIn(bucket.reset, nowEpochSeconds),
    )
}

/** "45s" or "2m 5s" until [resetEpochSeconds]; "0s" if it has already passed. */
internal fun formatResetsIn(
    resetEpochSeconds: Long,
    nowSeconds: Long = System.currentTimeMillis() / 1000,
): String {
    val secondsUntil = (resetEpochSeconds - nowSeconds).coerceAtLeast(0)
    return if (secondsUntil < 60) {
        "${secondsUntil}s"
    } else {
        "${secondsUntil / 60}m ${secondsUntil % 60}s"
    }
}
