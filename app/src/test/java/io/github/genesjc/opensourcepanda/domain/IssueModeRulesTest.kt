package io.github.genesjc.opensourcepanda.domain

import io.github.genesjc.opensourcepanda.model.IssueLabelFilter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IssueModeRulesTest {

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
