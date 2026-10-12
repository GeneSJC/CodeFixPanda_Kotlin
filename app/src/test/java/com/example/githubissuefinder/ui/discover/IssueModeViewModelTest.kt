package com.example.githubissuefinder.ui.discover

import com.example.githubissuefinder.data.IssueModeRepository
import com.example.githubissuefinder.data.IssueModeStorage
import com.example.githubissuefinder.model.IssueLabelFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IssueModeViewModelTest {

    private class FakeStorage : IssueModeStorage {
        var stored: String? = null
        override fun load(): String? = stored
        override fun save(name: String?) {
            stored = name
        }
    }

    @Test
    fun modeIsNullUntilOneIsSelected() {
        val viewModel = IssueModeViewModel(IssueModeRepository(FakeStorage()))
        assertNull(viewModel.mode.value)
    }

    @Test
    fun selectModeUpdatesObservedModeAndPersistsViaRepository() {
        val storage = FakeStorage()
        val viewModel = IssueModeViewModel(IssueModeRepository(storage))

        viewModel.selectMode(IssueLabelFilter.HELP_WANTED)

        assertEquals(IssueLabelFilter.HELP_WANTED, viewModel.mode.value)
        assertEquals(IssueLabelFilter.HELP_WANTED.name, storage.stored)
    }
}
