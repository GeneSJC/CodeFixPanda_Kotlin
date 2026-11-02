package io.github.genesjc.opensourcepanda.model

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
