package io.github.genesjc.githubissuefinder.data

import io.github.genesjc.githubissuefinder.model.IssueLabelFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IssueModeRulesTest {

    @Test
    fun resolveActiveMode_returnsTheChosenMode() {
        assertEquals(IssueLabelFilter.HELP_WANTED, resolveActiveMode(IssueLabelFilter.HELP_WANTED))
        assertEquals(IssueLabelFilter.GOOD_FIRST_ISSUE, resolveActiveMode(IssueLabelFilter.GOOD_FIRST_ISSUE))
    }

    @Test
    fun resolveActiveMode_fallsBackToTheDefaultWhenNoneChosen() {
        assertEquals(DEFAULT_ISSUE_MODE, resolveActiveMode(null))
    }

    @Test
    fun needsModeGate_isTrueOnlyWhileNoModeIsChosen() {
        assertTrue(needsModeGate(null))
        assertFalse(needsModeGate(IssueLabelFilter.GOOD_FIRST_ISSUE))
        assertFalse(needsModeGate(IssueLabelFilter.HELP_WANTED))
    }

    @Test
    fun shouldSwitchMode_isFalseForTheAlreadyActiveMode() {
        assertFalse(shouldSwitchMode(IssueLabelFilter.HELP_WANTED, IssueLabelFilter.HELP_WANTED))
    }

    @Test
    fun shouldSwitchMode_isTrueForADifferentMode() {
        assertTrue(shouldSwitchMode(IssueLabelFilter.GOOD_FIRST_ISSUE, IssueLabelFilter.HELP_WANTED))
    }

    @Test
    fun shouldSwitchMode_isTrueWhenNoModeWasChosenYet() {
        assertTrue(shouldSwitchMode(null, IssueLabelFilter.GOOD_FIRST_ISSUE))
    }
}
