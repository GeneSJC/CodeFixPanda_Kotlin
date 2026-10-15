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

    /**
     * TopReposViewModel and RepoIssuesViewModel talk to the repository
     * directly, so a change made there must show up in this ViewModel
     * (which drives the theme and start destination) too.
     */
    @Test
    fun modeReflectsChangesMadeDirectlyOnTheRepository() {
        val repository = IssueModeRepository(FakeStorage())
        val viewModel = IssueModeViewModel(repository)

        repository.setMode(IssueLabelFilter.GOOD_FIRST_ISSUE)
        assertEquals(IssueLabelFilter.GOOD_FIRST_ISSUE, viewModel.mode.value)

        repository.setMode(null)
        assertNull(viewModel.mode.value)
    }

    @Test
    fun twoViewModelsOverOneRepositoryAgree() {
        val repository = IssueModeRepository(FakeStorage())
        val first = IssueModeViewModel(repository)
        val second = IssueModeViewModel(repository)

        first.selectMode(IssueLabelFilter.HELP_WANTED)

        assertEquals(IssueLabelFilter.HELP_WANTED, second.mode.value)
    }

    @Test
    fun startsWithTheModeAlreadyStored() {
        val storage = FakeStorage().apply { stored = IssueLabelFilter.HELP_WANTED.name }
        val viewModel = IssueModeViewModel(IssueModeRepository(storage))
        assertEquals(IssueLabelFilter.HELP_WANTED, viewModel.mode.value)
    }
}
