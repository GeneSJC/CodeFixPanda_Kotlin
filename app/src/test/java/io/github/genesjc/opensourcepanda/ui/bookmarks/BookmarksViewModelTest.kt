package io.github.genesjc.opensourcepanda.ui.bookmarks

import io.github.genesjc.opensourcepanda.R
import io.github.genesjc.opensourcepanda.data.ApiTokenRepository
import io.github.genesjc.opensourcepanda.data.BookmarksRepository
import io.github.genesjc.opensourcepanda.data.BookmarksStorage
import io.github.genesjc.opensourcepanda.model.DataOrigin
import io.github.genesjc.opensourcepanda.model.Issue
import io.github.genesjc.opensourcepanda.testutil.FakeApiTokenStorage
import io.github.genesjc.opensourcepanda.testutil.FakeStringResolver
import io.github.genesjc.opensourcepanda.testutil.MainDispatcherRule
import io.github.genesjc.opensourcepanda.testutil.StopKoinRule
import io.github.genesjc.opensourcepanda.testutil.restartKoin
import io.github.genesjc.opensourcepanda.testutil.testAppConfig
import io.github.genesjc.opensourcepanda.ui.text.StringResolver
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
        var failSaves = false
        override fun load(): List<Issue> = stored
        override fun save(issues: List<Issue>, onFailure: () -> Unit) {
            if (failSaves) {
                onFailure()
                return
            }
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
            single<StringResolver> { FakeStringResolver }
        }
        return BookmarksViewModel() to repository
    }

    @Test
    fun aFailedSaveBecomesASnackbarMessage() = runBlocking {
        val storage = FakeStorage().apply { failSaves = true }
        val (viewModel, _) = viewModelOver(storage)
        val message = async(start = CoroutineStart.UNDISPATCHED) { viewModel.snackbarEvents.first() }

        viewModel.toggleBookmark(issue(1))

        assertEquals(FakeStringResolver(R.string.bookmark_save_failed), withTimeout(1_000) { message.await() })
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
        val (viewModel, _) = viewModelOver(FakeStorage(listOf(issue(1), issue(2).copy(origin = DataOrigin.DEMO))))

        assertEquals(listOf(1L), viewModel.bookmarks.value.map { it.id })
    }

    @Test
    fun withoutATokenOnlyDemoBookmarksAreListed() {
        val noToken = ApiTokenRepository(FakeApiTokenStorage(), buildToken = "")
        val (viewModel, _) = viewModelOver(FakeStorage(listOf(issue(1), issue(2).copy(origin = DataOrigin.DEMO))), noToken)

        assertEquals(listOf(2L), viewModel.bookmarks.value.map { it.id })
    }

    @Test
    fun theListSwitchesWhenATokenIsAdded() {
        val tokens = ApiTokenRepository(FakeApiTokenStorage(), buildToken = "")
        val (viewModel, _) = viewModelOver(FakeStorage(listOf(issue(1), issue(2).copy(origin = DataOrigin.DEMO))), tokens)

        tokens.setUserToken("token")

        assertEquals(listOf(1L), viewModel.bookmarks.value.map { it.id })
    }
}
