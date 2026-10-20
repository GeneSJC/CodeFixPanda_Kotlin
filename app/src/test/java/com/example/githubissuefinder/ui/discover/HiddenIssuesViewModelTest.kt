package com.example.githubissuefinder.ui.discover

import com.example.githubissuefinder.data.HiddenIssuesRepository
import com.example.githubissuefinder.data.HiddenIssuesStorage
import com.example.githubissuefinder.model.HiddenIssue
import com.example.githubissuefinder.model.Issue
import com.example.githubissuefinder.testutil.StopKoinRule
import com.example.githubissuefinder.testutil.restartKoin
import com.example.githubissuefinder.testutil.testAppConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HiddenIssuesViewModelTest {

    @get:Rule
    val stopKoin = StopKoinRule()

    private fun createViewModel(repository: HiddenIssuesRepository): HiddenIssuesViewModel {
        restartKoin {
            single { repository }
            single { testAppConfig() }
        }
        return HiddenIssuesViewModel()
    }

    private class FakeStorage(var stored: List<HiddenIssue> = emptyList()) : HiddenIssuesStorage {
        override fun load(): List<HiddenIssue> = stored
        override fun save(issues: List<HiddenIssue>) {
            stored = issues
        }
    }

    private fun issue(id: Long) = Issue(
        id = id,
        title = "Issue $id",
        bodyText = "",
        repoOwner = "octo",
        repoName = "repo",
        labels = emptyList(),
        language = null,
        commentCount = 0,
        updatedAtMs = null,
        htmlUrl = "https://github.com/octo/repo/issues/$id",
    )

    @Test
    fun listsWhatTheRepositoryHolds() {
        val repository = HiddenIssuesRepository(FakeStorage())
        repository.hide(issue(1))
        val viewModel = createViewModel(repository)
        assertEquals(listOf(1L), viewModel.hiddenIssues.value.map { it.id })
    }

    @Test
    fun unhideRemovesOneIssue() {
        val repository = HiddenIssuesRepository(FakeStorage())
        repository.hide(issue(1))
        repository.hide(issue(2))
        val viewModel = createViewModel(repository)

        viewModel.unhide(1)

        assertEquals(listOf(2L), viewModel.hiddenIssues.value.map { it.id })
    }

    @Test
    fun unhideAllEmptiesTheList() {
        val repository = HiddenIssuesRepository(FakeStorage())
        repository.hide(issue(1))
        repository.hide(issue(2))
        val viewModel = createViewModel(repository)

        viewModel.unhideAll()

        assertTrue(viewModel.hiddenIssues.value.isEmpty())
    }

    @Test
    fun reflectsIssuesHiddenElsewhere() {
        val repository = HiddenIssuesRepository(FakeStorage())
        val viewModel = createViewModel(repository)

        repository.hide(issue(4))

        assertEquals(listOf(4L), viewModel.hiddenIssues.value.map { it.id })
    }
}
