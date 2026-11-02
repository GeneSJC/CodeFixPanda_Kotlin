package io.github.genesjc.opensourcepanda.data

import io.github.genesjc.opensourcepanda.model.IssueLabelFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * IssueModeRepository against an in-memory fake storage — no Android
 * Context needed, which is the point of splitting IssueModeStorage out.
 */
class IssueModeRepositoryTest {

    private class FakeStorage(var stored: String? = null) : IssueModeStorage {
        var saveCount = 0
        override fun load(): String? = stored
        override fun save(name: String?) {
            stored = name
            saveCount++
        }
    }

    @Test
    fun startsNullWhenNothingStored() {
        val repository = IssueModeRepository(FakeStorage())
        assertNull(repository.mode.value)
    }

    @Test
    fun restoresStoredModeOnConstruction() {
        val repository = IssueModeRepository(FakeStorage(IssueLabelFilter.HELP_WANTED.name))
        assertEquals(IssueLabelFilter.HELP_WANTED, repository.mode.value)
    }

    @Test
    fun unrecognizedStoredNameRestoresAsNull() {
        val repository = IssueModeRepository(FakeStorage("NO_LONGER_A_MODE"))
        assertNull(repository.mode.value)
    }

    @Test
    fun requireModeReturnsTheChosenMode() {
        val repository = IssueModeRepository(FakeStorage(IssueLabelFilter.HELP_WANTED.name))
        assertEquals(IssueLabelFilter.HELP_WANTED, repository.requireMode())
    }

    @Test(expected = IllegalStateException::class)
    fun requireModeFailsWhenNoModeIsChosen() {
        IssueModeRepository(FakeStorage()).requireMode()
    }

    @Test
    fun setModeUpdatesStateAndPersists() {
        val storage = FakeStorage()
        val repository = IssueModeRepository(storage)

        repository.setMode(IssueLabelFilter.GOOD_FIRST_ISSUE)

        assertEquals(IssueLabelFilter.GOOD_FIRST_ISSUE, repository.mode.value)
        assertEquals(IssueLabelFilter.GOOD_FIRST_ISSUE.name, storage.stored)
    }

    @Test
    fun settingNullClearsStateAndStorage() {
        val storage = FakeStorage(IssueLabelFilter.HELP_WANTED.name)
        val repository = IssueModeRepository(storage)

        repository.setMode(null)

        assertNull(repository.mode.value)
        assertNull(storage.stored)
    }

    @Test
    fun construction_doesNotWriteToStorage() {
        val storage = FakeStorage(IssueLabelFilter.HELP_WANTED.name)
        IssueModeRepository(storage)
        assertEquals(0, storage.saveCount)
    }

    @Test
    fun settingTheSameModeTwiceKeepsTheValue() {
        val repository = IssueModeRepository(FakeStorage())
        repository.setMode(IssueLabelFilter.HELP_WANTED)
        repository.setMode(IssueLabelFilter.HELP_WANTED)
        assertEquals(IssueLabelFilter.HELP_WANTED, repository.mode.value)
    }

    @Test
    fun lastModeSetWins() {
        val repository = IssueModeRepository(FakeStorage())
        repository.setMode(IssueLabelFilter.GOOD_FIRST_ISSUE)
        repository.setMode(IssueLabelFilter.HELP_WANTED)
        assertEquals(IssueLabelFilter.HELP_WANTED, repository.mode.value)
    }

    /**
     * A new repository over the same storage stands in for the app
     * process being killed and relaunched: the choice must survive.
     */
    @Test
    fun aNewRepositoryOverTheSameStorageRestoresTheLastChoice() {
        val storage = FakeStorage()
        IssueModeRepository(storage).setMode(IssueLabelFilter.HELP_WANTED)

        val afterRestart = IssueModeRepository(storage)

        assertEquals(IssueLabelFilter.HELP_WANTED, afterRestart.mode.value)
    }

    @Test
    fun aClearedModeStaysClearedAfterARestart() {
        val storage = FakeStorage()
        val first = IssueModeRepository(storage)
        first.setMode(IssueLabelFilter.GOOD_FIRST_ISSUE)
        first.setMode(null)

        assertNull(IssueModeRepository(storage).mode.value)
    }
}
