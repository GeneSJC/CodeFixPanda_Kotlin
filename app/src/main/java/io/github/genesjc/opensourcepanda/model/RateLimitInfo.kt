package io.github.genesjc.opensourcepanda.model

/**
 * remaining/limit/used as reported by GitHub's Search API. That API has its own, stricter limit,
 * separate from the general REST API's 5000/hour, so these numbers are for search only. Its window is
 * short (per minute), which is why the reset is kept in seconds. [resetsInSeconds] is how long until
 * the window resets when the numbers were read, or null if GitHub did not say; the UI words it
 * (RateLimitText.kt).
 */
data class RateLimitInfo(
    val remaining: Int,
    val limit: Int,
    val used: Int,
    val resetsInSeconds: Long?,
)
