package io.github.genesjc.githubissuefinder.ui

import io.github.genesjc.githubissuefinder.R
import io.github.genesjc.githubissuefinder.testutil.FakeStringResolver
import org.junit.Assert.assertEquals
import org.junit.Test

/** The wording of the seconds until the rate limit resets, over a fake resolver. */
class RateLimitTextTest {

    private val s = FakeStringResolver

    @Test
    fun underAMinuteIsShownInSeconds() {
        assertEquals(s(R.string.resets_in_seconds, 59L), s.resetsIn(59L))
        assertEquals(s(R.string.resets_in_seconds, 0L), s.resetsIn(0L))
    }

    @Test
    fun aMinuteOrMoreIsShownAsMinutesAndSeconds() {
        assertEquals(s(R.string.resets_in_minutes_seconds, 1L, 0L), s.resetsIn(60L))
        assertEquals(s(R.string.resets_in_minutes_seconds, 2L, 5L), s.resetsIn(125L))
    }

    @Test
    fun anUnknownResetIsUnknown() {
        assertEquals(s(R.string.resets_in_unknown), s.resetsIn(null))
    }
}
