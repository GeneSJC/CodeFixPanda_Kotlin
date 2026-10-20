package io.github.genesjc.githubissuefinder.ui.discover

import io.github.genesjc.githubissuefinder.R
import io.github.genesjc.githubissuefinder.data.AppConfig
import io.github.genesjc.githubissuefinder.data.RateLimitInfo
import io.github.genesjc.githubissuefinder.data.ReadmeFetch
import io.github.genesjc.githubissuefinder.data.RepoIssueGroupCacheEntry
import io.github.genesjc.githubissuefinder.data.TOP_REPOS_PER_PAGE
import io.github.genesjc.githubissuefinder.data.cacheKeyFor
import io.github.genesjc.githubissuefinder.data.topReposQuery
import io.github.genesjc.githubissuefinder.model.IssueLabelFilter
import io.github.genesjc.githubissuefinder.model.LanguageFilter
import io.github.genesjc.githubissuefinder.testutil.FakeReadmeSource
import io.github.genesjc.githubissuefinder.testutil.FakeStringResolver
import io.github.genesjc.githubissuefinder.testutil.MainDispatcherRule
import io.github.genesjc.githubissuefinder.testutil.StopKoinRule
import io.github.genesjc.githubissuefinder.testutil.ViewModelFixtures
import io.github.genesjc.githubissuefinder.testutil.issueDto
import io.github.genesjc.githubissuefinder.testutil.repoGroupOf
import io.github.genesjc.githubissuefinder.testutil.searchFailed
import io.github.genesjc.githubissuefinder.testutil.searchOk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * TopReposViewModel over the real TopReposStateHolder (built on
 * fakes). What the state holder
 * decides is covered in TopReposStateHolderTest; this covers what the
 * ViewModel adds: the message text, the forwarding of actions, and the
 * pass-throughs for the repo cards.
 */
class TopReposViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    @get:Rule
    val stopKoin = StopKoinRule()

    private val s = FakeStringResolver

    private val gfi = IssueLabelFilter.GOOD_FIRST_ISSUE
    private val kotlin = LanguageFilter.KOTLIN

    /** A ViewModel whose state holder is the one built over [f]'s fakes. */
    private fun viewModel(f: ViewModelFixtures, config: AppConfig = f.appConfig): TopReposViewModel =
        f.topReposViewModel(config)

    private fun fullPage() = (1L..TOP_REPOS_PER_PAGE).map { issueDto(it, "a", "one") }

    // ---- state with message text ----

    @Test
    fun constructingItShowsTheCachedList() {
        val f = ViewModelFixtures()
        val group = repoGroupOf("a", "one", 1, 2)
        f.cacheStorage.data = mapOf(
            cacheKeyFor(kotlin, gfi) to RepoIssueGroupCacheEntry(listOf(group), 5, 2, true, 777L),
        )

        val state = viewModel(f).uiState.value

        assertEquals(listOf(group), state.repoGroups)
        assertEquals(777L, state.lastUpdatedAtMs)
        assertFalse(state.isLoading)
        assertTrue(f.api.calls.isEmpty())
    }

    @Test
    fun aRejectedTokenShowsItsMessageAndFlag() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> searchFailed(401) }

        val state = viewModel(f).uiState.value

        assertTrue(state.tokenRejected)
        assertEquals(s(R.string.error_http, 401), state.errorMessage)
    }

    @Test
    fun aFailedRequestShowsWhatWentWrong() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> throw java.io.IOException("boom") }

        assertEquals(s(R.string.load_issues_failed, "boom"), viewModel(f).uiState.value.errorMessage)
    }

    @Test
    fun aFailedLoadMoreShowsItsMessageInTheFooter() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, page -> if (page == 1) searchOk(fullPage(), total = 200) else searchFailed(500) }
        val vm = viewModel(f)

        vm.loadMore()

        assertEquals(s(R.string.error_http, 500), vm.uiState.value.loadMoreError)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun theRateLimitComesFromTheSearchRepository() {
        val f = ViewModelFixtures()
        val limit = RateLimitInfo(remaining = 29, limit = 30, used = 1, resetsInSeconds = 45L)
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1)), rateLimit = limit) }

        val vm = viewModel(f)

        assertEquals(limit, vm.rateLimit.value)
    }

    @Test
    fun theDemoBannerIsOnExactlyWhileThereIsNoToken() {
        assertTrue(viewModel(ViewModelFixtures(buildToken = "")).isDemo.value)
        assertFalse(viewModel(ViewModelFixtures()).isDemo.value)
    }

    @Test
    fun withoutATokenHasTokenIsFalse() {
        val f = ViewModelFixtures(buildToken = "")

        val vm = viewModel(f)

        assertFalse(vm.hasToken.value)
    }

    // ---- snackbar text ----

    @Test
    fun theRateLimitedLoadMoreSaysSo() = runBlocking {
        val f = ViewModelFixtures()
        val exhausted = RateLimitInfo(remaining = 0, limit = 30, used = 30, resetsInSeconds = 40L)
        f.api.handler = { _, _, page -> if (page == 1) searchOk(fullPage(), total = 200) else searchFailed(403, exhausted) }
        val vm = viewModel(f)

        vm.loadMore()

        assertEquals(s(R.string.search_rate_limited), vm.snackbarEvents.first())
    }

    @Test
    fun checkingForNewReposSaysWhatIsNew() = runBlocking {
        val f = ViewModelFixtures()
        f.cacheStorage.data = mapOf(
            cacheKeyFor(kotlin, gfi) to RepoIssueGroupCacheEntry(listOf(repoGroupOf("a", "one", 1)), 5, 1, true, 777L),
        )
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1, "a", "one"), issueDto(2, "b", "two")), total = 9) }
        val vm = viewModel(f)

        vm.checkForNewRepos()

        assertEquals(
            s(R.string.new_repos_and_issues, s.plural(R.plurals.new_repos, 1, 1), s.plural(R.plurals.new_issues, 1, 1)),
            vm.snackbarEvents.first(),
        )
    }

    // ---- forwarding ----

    @Test
    fun theModeAndLanguageSwitchesReachTheStateHolder() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1))) }
        val vm = viewModel(f)

        vm.onModeSelected(IssueLabelFilter.HELP_WANTED)
        assertEquals(IssueLabelFilter.HELP_WANTED, vm.mode.value)

        vm.onLanguageSelected(LanguageFilter.JAVA)
        assertEquals(LanguageFilter.JAVA, vm.uiState.value.selectedLanguage)
        assertEquals(topReposQuery(IssueLabelFilter.HELP_WANTED, LanguageFilter.JAVA), f.api.calls.last().first)
    }

    @Test
    fun clearingTheCacheLoadsFreshData() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1))) }
        val vm = viewModel(f)
        assertEquals(1, f.api.calls.size)

        vm.clearCache()

        assertEquals(2, f.api.calls.size)
    }

    // ---- pass-throughs for the repo cards ----

    @Test
    fun aRepoCardAppearingLoadsItsReadmePreview() {
        val f = ViewModelFixtures(readmeSource = FakeReadmeSource(ReadmeFetch.Resolved("# Hello\n\nWorld")))
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1))) }
        val vm = viewModel(f)

        vm.onRepoCardShown("octo", "repo", "octo/repo")

        assertEquals("Hello World", vm.snippets.value["octo/repo"])
    }

    @Test
    fun hiddenIdsComeFromTheHiddenIssuesRepository() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1))) }
        val vm = viewModel(f)
        assertTrue(vm.hiddenIds.value.isEmpty())

        f.hiddenIssuesRepository.hide(repoGroupOf("octo", "repo", 1).issues.single())

        assertEquals(setOf(1L), vm.hiddenIds.value)
    }

    @Test
    fun configValuesComeFromTheAppConfig() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1))) }
        val config = AppConfig(buildLabel = "9.9", splashAnimation = "shapeStack", showDetailedDrawerMenu = true)
        val vm = viewModel(f, config)

        assertEquals("9.9", vm.buildLabel)
        assertEquals("shapeStack", vm.splashAnimation)
        assertTrue(vm.showDetailedDrawerMenu)
    }
}
