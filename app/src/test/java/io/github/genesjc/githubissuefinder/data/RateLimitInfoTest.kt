package io.github.genesjc.githubissuefinder.data

import io.github.genesjc.githubissuefinder.network.dto.RateLimitBucketDto
import io.github.genesjc.githubissuefinder.network.dto.RateLimitResourcesDto
import io.github.genesjc.githubissuefinder.network.dto.RateLimitResponseDto
import org.junit.Assert.assertEquals
import org.junit.Test

class RateLimitInfoTest {

    private fun response(limit: Int = 30, remaining: Int = 29, used: Int = 1, reset: Long) =
        RateLimitResponseDto(RateLimitResourcesDto(RateLimitBucketDto(limit, remaining, used, reset)))

    @Test
    fun mapsTheSearchBucketOfARateLimitResponse() {
        val info = response(limit = 30, remaining = 29, used = 1, reset = 1_000_045L)
            .toSearchRateLimitInfo(nowEpochSeconds = 1_000_000L)

        assertEquals(29, info.remaining)
        assertEquals(30, info.limit)
        assertEquals(1, info.used)
        assertEquals(45L, info.resetsInSeconds)
    }

    @Test
    fun secondsUntilResetCountsFromNow() {
        assertEquals(59L, secondsUntilReset(resetEpochSeconds = 1059, nowSeconds = 1000))
        assertEquals(125L, secondsUntilReset(resetEpochSeconds = 1125, nowSeconds = 1000))
    }

    @Test
    fun aResetTimeInThePastIsZeroSeconds() {
        assertEquals(0L, secondsUntilReset(resetEpochSeconds = 900, nowSeconds = 1000))
    }
}
