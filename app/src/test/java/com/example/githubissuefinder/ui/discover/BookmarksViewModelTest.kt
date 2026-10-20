package com.example.githubissuefinder.ui.discover

import com.example.githubissuefinder.testutil.MainDispatcherRule
import com.example.githubissuefinder.testutil.FakeApiTokenStorage
import com.example.githubissuefinder.model.DEMO_TAG
import com.example.githubissuefinder.data.ApiTokenRepository
import com.example.githubissuefinder.data.BookmarksRepository
import com.example.githubissuefinder.data.BookmarksStorage
import com.example.githubissuefinder.model.Issue
import com.example.githubissuefinder.testutil.StopKoinRule
import com.example.githubissuefinder.testutil.restartKoin
import com.example.githubissuefinder.testutil.testAppConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BookmarksViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    @get:Rule
    val stopKoin = StopKoinRule()

    private class FakeStorage(var stored: List<Issue> = emptyList()) : BookmarksStorage {
        override fun load(): List<Issue> = stored
        override fun save(issues: List<Issue>) {
            stored = issues
        }
    }

    /** A ViewModel over a repository on [storage]. */
    private fun viewModelOver(
        storage: FakeStorage,
        tokens: ApiTokenRepository = ApiTokenRepository(FakeApiTokenStorage("token"), buildToken = ""),
    ): Pair<BookmarksViewModel, BookmarksRepository> {
        val repository = BookmarksRepository(storage)
        restartKoin {
            single { repository }
            single { tokens }
            single { testAppConfig() }
        }
        return BookmarksViewModel() to repository
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
        val (viewModel, _) = viewModelOver(FakeStorage(listOf(issue(1), issue(2))))

        assertEquals(listOf(1L, 2L), viewModel.bookmarks.value.map { it.id })
    }

    @Test
    fun toggleBookmarkAddsAndReportsTheNewState() {
        val (viewModel, _) = viewModelOver(FakeStorage())

        assertTrue(viewModel.toggleBookmark(issue(1)))

        assertEquals(listOf(1L), viewModel.bookmarks.value.map { it.id })
    }

    @Test
    fun toggleBookmarkRemovesAnExistingBookmark() {
        val (viewModel, _) = viewModelOver(FakeStorage(listOf(issue(1))))

        assertFalse(viewModel.toggleBookmark(issue(1)))

        assertTrue(viewModel.bookmarks.value.isEmpty())
    }

    @Test
    fun reflectsBookmarksMadeElsewhere() {
        val (viewModel, repository) = viewModelOver(FakeStorage())

        repository.toggle(issue(4))

        assertEquals(listOf(4L), viewModel.bookmarks.value.map { it.id })
    }

    @Test
    fun withATokenOnlyUntaggedBookmarksAreListed() {
        val (viewModel, _) = viewModelOver(FakeStorage(listOf(issue(1), issue(2).copy(tag = DEMO_TAG))))

        assertEquals(listOf(1L), viewModel.bookmarks.value.map { it.id })
    }

    @Test
    fun withoutATokenOnlyDemoBookmarksAreListed() {
        val noToken = ApiTokenRepository(FakeApiTokenStorage(), buildToken = "")
        val (viewModel, _) = viewModelOver(FakeStorage(listOf(issue(1), issue(2).copy(tag = DEMO_TAG))), noToken)

        assertEquals(listOf(2L), viewModel.bookmarks.value.map { it.id })
    }

    @Test
    fun theListSwitchesWhenATokenIsAdded() {
        val tokens = ApiTokenRepository(FakeApiTokenStorage(), buildToken = "")
        val (viewModel, _) = viewModelOver(FakeStorage(listOf(issue(1), issue(2).copy(tag = DEMO_TAG))), tokens)

        tokens.setUserToken("token")

        assertEquals(listOf(1L), viewModel.bookmarks.value.map { it.id })
    }
}
