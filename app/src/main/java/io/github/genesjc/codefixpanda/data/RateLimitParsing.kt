package io.github.genesjc.codefixpanda.data

import io.github.genesjc.codefixpanda.data.network.dto.RateLimitResponseDto
import io.github.genesjc.codefixpanda.model.RateLimitInfo
import okhttp3.Headers

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
