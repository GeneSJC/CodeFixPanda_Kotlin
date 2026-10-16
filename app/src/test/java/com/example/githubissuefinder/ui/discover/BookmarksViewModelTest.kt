package com.example.githubissuefinder.ui.discover

import com.example.githubissuefinder.data.BookmarksRepository
import com.example.githubissuefinder.data.BookmarksStorage
import com.example.githubissuefinder.model.Issue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookmarksViewModelTest {

    private class FakeStorage(var stored: List<Issue> = emptyList()) : BookmarksStorage {
        override fun load(): List<Issue> = stored
        override fun save(issues: List<Issue>) {
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
        updatedAtLabel = "just now",
        htmlUrl = "https://github.com/octo/repo/issues/$id",
    )

    @Test
    fun listsWhatTheRepositoryHolds() {
        val repository = BookmarksRepository(FakeStorage(listOf(issue(1), issue(2))))
        val viewModel = BookmarksViewModel(repository)
        assertEquals(listOf(1L, 2L), viewModel.bookmarks.value.map { it.id })
    }

    @Test
    fun toggleBookmarkAddsAndReportsTheNewState() {
        val viewModel = BookmarksViewModel(BookmarksRepository(FakeStorage()))

        assertTrue(viewModel.toggleBookmark(issue(1)))

        assertEquals(listOf(1L), viewModel.bookmarks.value.map { it.id })
    }

    @Test
    fun toggleBookmarkRemovesAnExistingBookmark() {
        val viewModel = BookmarksViewModel(BookmarksRepository(FakeStorage(listOf(issue(1)))))

        assertFalse(viewModel.toggleBookmark(issue(1)))

        assertTrue(viewModel.bookmarks.value.isEmpty())
    }

    @Test
    fun reflectsBookmarksMadeElsewhere() {
        val repository = BookmarksRepository(FakeStorage())
        val viewModel = BookmarksViewModel(repository)

        repository.toggle(issue(4))

        assertEquals(listOf(4L), viewModel.bookmarks.value.map { it.id })
    }
}
