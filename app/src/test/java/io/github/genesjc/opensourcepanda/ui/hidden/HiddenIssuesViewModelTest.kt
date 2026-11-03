package io.github.genesjc.opensourcepanda.ui.hidden

import io.github.genesjc.opensourcepanda.R
import io.github.genesjc.opensourcepanda.data.ApiTokenRepository
import io.github.genesjc.opensourcepanda.data.HiddenIssuesRepository
import io.github.genesjc.opensourcepanda.data.HiddenIssuesStorage
import io.github.genesjc.opensourcepanda.model.DataOrigin
import io.github.genesjc.opensourcepanda.model.HiddenIssue
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HiddenIssuesViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    @get:Rule
    val stopKoin = StopKoinRule()

    /** A ViewModel over [repository]; [tokens] decides whether the real or the demo data is in use (a real token by default). */
    private fun createViewModel(
        repository: HiddenIssuesRepository,
        tokens: ApiTokenRepository = ApiTokenRepository(FakeApiTokenStorage("token"), buildToken = ""),
    ): HiddenIssuesViewModel {
        restartKoin {
            single { repository }
            single { tokens }
            single { testAppConfig() }
            single<StringResolver> { FakeStringResolver }
        }

        return HiddenIssuesViewModel()
    }

    private class FakeStorage(var stored: List<HiddenIssue> = emptyList()) : HiddenIssuesStorage {
        var failSaves = false
        override fun load(): List<HiddenIssue> = stored
        override fun save(issues: List<HiddenIssue>, onFailure: () -> Unit) {
            if (failSaves) {
                onFailure()
                return
            }

            stored = issues
        }
    }

    @Test
    fun aFailedSaveBecomesASnackbarMessage() = runBlocking {
        val repository = HiddenIssuesRepository(FakeStorage().apply { failSaves = true })
        val viewModel = createViewModel(repository)
        val message = async(start = CoroutineStart.UNDISPATCHED) { viewModel.snackbarEvents.first() }

        repository.hide(issue(1))

        assertEquals(FakeStringResolver(R.string.hidden_save_failed), withTimeout(1_000) { message.await() })
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

    @Test
    fun withATokenOnlyUntaggedIssuesAreListed() {
        val repository = HiddenIssuesRepository(FakeStorage())
        repository.hide(issue(1))
        repository.hide(issue(2).copy(origin = DataOrigin.DEMO))

        val viewModel = createViewModel(repository)

        assertEquals(listOf(1L), viewModel.hiddenIssues.value.map { it.id })
    }

    @Test
    fun withoutATokenOnlyDemoIssuesAreListed() {
        val repository = HiddenIssuesRepository(FakeStorage())
        repository.hide(issue(1))
        repository.hide(issue(2).copy(origin = DataOrigin.DEMO))
        val noToken = ApiTokenRepository(FakeApiTokenStorage(), buildToken = "")

        val viewModel = createViewModel(repository, noToken)

        assertEquals(listOf(2L), viewModel.hiddenIssues.value.map { it.id })
    }

    @Test
    fun theListSwitchesWhenATokenIsAdded() {
        val repository = HiddenIssuesRepository(FakeStorage())
        repository.hide(issue(1))
        repository.hide(issue(2).copy(origin = DataOrigin.DEMO))
        val tokens = ApiTokenRepository(FakeApiTokenStorage(), buildToken = "")
        val viewModel = createViewModel(repository, tokens)

        tokens.setUserToken("token")

        assertEquals(listOf(1L), viewModel.hiddenIssues.value.map { it.id })
    }

    @Test
    fun unhideAllOnlyTouchesTheDataInUse() {
        val repository = HiddenIssuesRepository(FakeStorage())
        repository.hide(issue(1))
        repository.hide(issue(2).copy(origin = DataOrigin.DEMO))
        val viewModel = createViewModel(repository)

        viewModel.unhideAll()

        assertTrue(viewModel.hiddenIssues.value.isEmpty())
        assertEquals(listOf(2L), repository.hiddenIssues.value.map { it.id })
    }
}
