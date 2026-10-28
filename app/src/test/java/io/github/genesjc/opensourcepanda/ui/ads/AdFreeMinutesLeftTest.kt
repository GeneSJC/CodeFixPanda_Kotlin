package io.github.genesjc.opensourcepanda.ui.ads

import org.junit.Assert.assertEquals
import org.junit.Test

class AdFreeMinutesLeftTest {

    private val now = 1_000_000L

    @Test
    fun aFullHourReadsSixtyMinutes() {
        assertEquals(60, adFreeMinutesLeft(now + 60 * 60_000L, now))
    }

    @Test
    fun aPartialMinuteRoundsUp() {
        assertEquals(1, adFreeMinutesLeft(now + 1, now))
        assertEquals(2, adFreeMinutesLeft(now + 60_001L, now))
    }

    @Test
    fun noPeriodOrAnEndedOneReadsZero() {
        assertEquals(0, adFreeMinutesLeft(0L, now))
        assertEquals(0, adFreeMinutesLeft(now, now))
        assertEquals(0, adFreeMinutesLeft(now - 5_000L, now))
    }
}
