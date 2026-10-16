package com.example.githubissuefinder.data

import com.example.githubissuefinder.network.dto.RateLimitBucketDto
import com.example.githubissuefinder.network.dto.RateLimitResourcesDto
import com.example.githubissuefinder.network.dto.RateLimitResponseDto
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
        assertEquals("45s", info.resetsInLabel)
    }

    @Test
    fun resetsInUnderAMinuteIsShownInSeconds() {
        assertEquals("59s", formatResetsIn(resetEpochSeconds = 1059, nowSeconds = 1000))
    }

    @Test
    fun resetsInAMinuteOrMoreIsShownAsMinutesAndSeconds() {
        assertEquals("1m 0s", formatResetsIn(resetEpochSeconds = 1060, nowSeconds = 1000))
        assertEquals("2m 5s", formatResetsIn(resetEpochSeconds = 1125, nowSeconds = 1000))
    }

    @Test
    fun aResetTimeInThePastIsShownAsZeroSeconds() {
        assertEquals("0s", formatResetsIn(resetEpochSeconds = 900, nowSeconds = 1000))
    }
}
