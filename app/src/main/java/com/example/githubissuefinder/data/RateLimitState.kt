package com.example.githubissuefinder.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
 * Plain in-memory singleton — same temporary, not-Hilt-managed pattern as
 * GitHubApiClient. Null means "no live search has completed yet," or
 * "currently in sample-data mode" (no token configured), where a rate
 * limit isn't applicable at all.
 *
 * Deliberately independent of the UI state types built from search
 * results (e.g. TopReposUiState): this reflects account/session state
 * (the credential's shared quota), not anything about a particular
 * search's results.
 */
object RateLimitState {

    private val _info = MutableStateFlow<RateLimitInfo?>(null)
    val info: StateFlow<RateLimitInfo?> = _info.asStateFlow()

    fun update(newInfo: RateLimitInfo?) {
        _info.value = newInfo
    }
}

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

private fun formatResetsIn(resetEpochSeconds: Long): String {
    val nowSeconds = System.currentTimeMillis() / 1000
    val secondsUntil = (resetEpochSeconds - nowSeconds).coerceAtLeast(0)
    return if (secondsUntil < 60) {
        "${secondsUntil}s"
    } else {
        "${secondsUntil / 60}m ${secondsUntil % 60}s"
    }
}
