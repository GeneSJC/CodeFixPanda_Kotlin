package com.example.githubissuefinder.data

import com.example.githubissuefinder.model.IssueLabelFilter
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
}
