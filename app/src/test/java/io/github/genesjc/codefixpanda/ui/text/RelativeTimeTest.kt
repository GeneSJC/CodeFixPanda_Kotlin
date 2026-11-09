package io.github.genesjc.codefixpanda.ui.text

import io.github.genesjc.codefixpanda.R
import io.github.genesjc.codefixpanda.testutil.FakeStringResolver
import org.junit.Assert.assertEquals
import org.junit.Test

/** The wording buckets for "how long ago", over a fake resolver. */
class RelativeTimeTest {

    private val s = FakeStringResolver
    private val now = 100L * 60 * 60 * 1000
    private val hour = 60L * 60 * 1000

    @Test
    fun underAnHourIsJustNow() {
        assertEquals(s(R.string.time_just_now), s.relativeTime(now - 59 * 60 * 1000, now))
    }

    @Test
    fun underADayCountsHours() {
        assertEquals(s(R.string.time_hours_ago, 1L), s.relativeTime(now - hour, now))
        assertEquals(s(R.string.time_hours_ago, 23L), s.relativeTime(now - 23 * hour, now))
    }

    @Test
    fun aDayOrMoreCountsDays() {
        assertEquals(s(R.string.time_days_ago, 1L), s.relativeTime(now - 24 * hour, now))
        assertEquals(s(R.string.time_days_ago, 3L), s.relativeTime(now - 80 * hour, now))
    }

    @Test
    fun anUnknownTimeIsRecently() {
        assertEquals(s(R.string.time_recently), s.relativeTime(null, now))
    }
}
