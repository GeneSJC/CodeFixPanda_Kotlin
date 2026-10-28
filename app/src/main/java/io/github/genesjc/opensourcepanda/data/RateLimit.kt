package io.github.genesjc.opensourcepanda.data

import io.github.genesjc.opensourcepanda.network.dto.RateLimitResponseDto
import okhttp3.Headers

/**
 * remaining/limit/used as reported by GitHub's Search API on its own
 * responses. Worth noting: the Search API has its own, separate, stricter
 * rate limit from GitHub's general REST API — these numbers reflect
 * search specifically, not the broader 5000/hour core API figure some
 * might expect. That's also why the reset is kept in seconds, not hours —
 * the Search API's window is short (per-minute), so an hour-granularity
 * figure would be misleadingly imprecise here. [resetsInSeconds] is how
 * long until the window resets when the numbers were read, or null if
 * GitHub did not say; the UI words it (RateLimitText.kt).
 */
data class RateLimitInfo(
    val remaining: Int,
    val limit: Int,
    val used: Int,
    val resetsInSeconds: Long?,
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
    val resetsInSeconds = resetEpochSeconds?.let { secondsUntilReset(it) }
    return RateLimitInfo(remaining, limit, used, resetsInSeconds)
}

/**
 * The Search API rate-limit state from a GET /rate_limit response, used
 * to fill in the chip when a list was loaded from the cache and no
 * search response has supplied the numbers yet. [nowEpochSeconds] is a
 * parameter so the seconds until reset can be tested.
 */
fun RateLimitResponseDto.toSearchRateLimitInfo(
    nowEpochSeconds: Long = System.currentTimeMillis() / 1000,
): RateLimitInfo {
    val bucket = resources.search
    return RateLimitInfo(
        remaining = bucket.remaining,
        limit = bucket.limit,
        used = bucket.used,
        resetsInSeconds = secondsUntilReset(bucket.reset, nowEpochSeconds),
    )
}

/** Seconds until [resetEpochSeconds]; 0 if it has already passed. */
internal fun secondsUntilReset(
    resetEpochSeconds: Long,
    nowSeconds: Long = System.currentTimeMillis() / 1000,
): Long = (resetEpochSeconds - nowSeconds).coerceAtLeast(0)
