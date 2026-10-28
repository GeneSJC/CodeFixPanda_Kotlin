package io.github.genesjc.opensourcepanda.network.dto

/**
 * Raw shape of GET /rate_limit, which does not count against any limit, so the app can use it to
 * fill in the rate-limit chip without spending a search. Only the Search API's bucket is modeled;
 * Moshi ignores the rest.
 */
data class RateLimitResponseDto(
    val resources: RateLimitResourcesDto,
)

data class RateLimitResourcesDto(
    val search: RateLimitBucketDto,
)

/** One bucket: the quota, what is left, what is used, and when it resets (epoch seconds). */
data class RateLimitBucketDto(
    val limit: Int,
    val remaining: Int,
    val used: Int,
    val reset: Long,
)
