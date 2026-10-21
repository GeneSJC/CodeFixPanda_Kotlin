package io.github.genesjc.githubissuefinder.ads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdFreePeriodTest {

    private class FakeStorage(var stored: Long = 0L) : AdFreeStorage {
        override fun load(): Long = stored
        override fun save(untilMs: Long) {
            stored = untilMs
        }
    }

    private val start = 1_000_000L
    private val period = 60_000L

    @Test
    fun noPeriodIsRunningToBeginWith() {
        val p = AdFreePeriod(FakeStorage(), { start }, period)
        assertFalse(p.isActive())
    }

    @Test
    fun startingBeginsAPeriodAndSavesItsEnd() {
        val storage = FakeStorage()
        val p = AdFreePeriod(storage, { start }, period)

        assertTrue(p.start())

        assertTrue(p.isActive())
        assertEquals(start + period, p.untilMs.value)
        assertEquals(start + period, storage.stored)
    }

    @Test
    fun startingAgainWhileRunningChangesNothing() {
        var now = start
        val p = AdFreePeriod(FakeStorage(), { now }, period)
        p.start()
        now += 10_000

        assertFalse(p.start())

        assertEquals(start + period, p.untilMs.value)
    }

    @Test
    fun aPeriodEndsAtItsEndTimeAndCanThenBeStartedAgain() {
        var now = start
        val p = AdFreePeriod(FakeStorage(), { now }, period)
        p.start()

        now = start + period

        assertFalse(p.isActive())
        assertTrue(p.start())
        assertEquals(now + period, p.untilMs.value)
    }

    @Test
    fun aSavedPeriodSurvivesARestart() {
        val storage = FakeStorage()
        AdFreePeriod(storage, { start }, period).start()

        val afterRestart = AdFreePeriod(storage, { start + 5_000 }, period)

        assertTrue(afterRestart.isActive())
        assertEquals(start + period, afterRestart.untilMs.value)
    }

    @Test
    fun aSavedEndFurtherAheadThanOnePeriodIsCutBack() {
        val storage = FakeStorage(stored = start + 10 * period)

        val p = AdFreePeriod(storage, { start }, period)

        assertEquals(start + period, p.untilMs.value)
    }

    @Test
    fun activeFlowTurnsFalseOnItsOwnAtTheEnd() = runTest {
        val p = AdFreePeriod(FakeStorage(), { start + testScheduler.currentTime }, period)
        val seen = mutableListOf<Boolean>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { p.active.toList(seen) }

        assertEquals(listOf(false), seen)

        p.start()
        assertEquals(listOf(false, true), seen)

        advanceTimeBy(period - 1)
        runCurrent()
        assertEquals(listOf(false, true), seen)

        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf(false, true, false), seen)
    }
}
