package com.example.githubissuefinder.ui.discover

import com.example.githubissuefinder.data.IssueModeRepository
import com.example.githubissuefinder.data.IssueModeStorage
import com.example.githubissuefinder.model.IssueLabelFilter
import com.example.githubissuefinder.testutil.testAppConfigRepository
import com.example.githubissuefinder.testutil.testBookmarksRepository
import com.example.githubissuefinder.testutil.testHiddenIssuesRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IssueModeViewModelTest {

    private fun createViewModel(repository: IssueModeRepository) =
        IssueModeViewModel(repository, testAppConfigRepository(), testHiddenIssuesRepository(), testBookmarksRepository())

    private class FakeStorage : IssueModeStorage {
        var stored: String? = null
        override fun load(): String? = stored
        override fun save(name: String?) {
            stored = name
        }
    }

    @Test
    fun modeIsNullUntilOneIsSelected() {
        val viewModel = createViewModel(IssueModeRepository(FakeStorage()))
        assertNull(viewModel.mode.value)
    }

    @Test
    fun selectModeUpdatesObservedModeAndPersistsViaRepository() {
        val storage = FakeStorage()
        val viewModel = createViewModel(IssueModeRepository(storage))

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
        val viewModel = createViewModel(repository)

        repository.setMode(IssueLabelFilter.GOOD_FIRST_ISSUE)
        assertEquals(IssueLabelFilter.GOOD_FIRST_ISSUE, viewModel.mode.value)

        repository.setMode(null)
        assertNull(viewModel.mode.value)
    }

    @Test
    fun clearModeResetsToNoneAndClearsStorage() {
        val storage = FakeStorage()
        val viewModel = createViewModel(IssueModeRepository(storage))
        viewModel.selectMode(IssueLabelFilter.HELP_WANTED)

        viewModel.clearMode()

        assertNull(viewModel.mode.value)
        assertNull(storage.stored)
    }

    @Test
    fun twoViewModelsOverOneRepositoryAgree() {
        val repository = IssueModeRepository(FakeStorage())
        val first = createViewModel(repository)
        val second = createViewModel(repository)

        first.selectMode(IssueLabelFilter.HELP_WANTED)

        assertEquals(IssueLabelFilter.HELP_WANTED, second.mode.value)
    }

    @Test
    fun startsWithTheModeAlreadyStored() {
        val storage = FakeStorage().apply { stored = IssueLabelFilter.HELP_WANTED.name }
        val viewModel = createViewModel(IssueModeRepository(storage))
        assertEquals(IssueLabelFilter.HELP_WANTED, viewModel.mode.value)
    }
}
