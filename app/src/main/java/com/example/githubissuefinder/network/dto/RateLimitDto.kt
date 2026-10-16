package com.example.githubissuefinder.network.dto

/**
 * Raw shape returned by GET /rate_limit — GitHub's endpoint for checking
 * every rate-limit bucket at once. Calling it does NOT count against any
 * of those limits, which is why the app can use it to fill in the
 * rate-limit chip without spending a search.
 *
 * Only the Search API's bucket is modeled: that is the one
 * IssueSearchRepository.rateLimit tracks (see its doc comment for why the general REST
 * bucket is deliberately kept out). Moshi ignores the other buckets in
 * the response.
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
