package com.example.githubissuefinder.ui.discover

import com.example.githubissuefinder.data.RateLimitInfo
import com.example.githubissuefinder.data.RateLimitState
import com.example.githubissuefinder.data.ReadmeFetch
import com.example.githubissuefinder.data.RepoIssueGroupCacheEntry
import com.example.githubissuefinder.data.TOP_REPOS_PER_PAGE
import com.example.githubissuefinder.data.cacheKeyFor
import com.example.githubissuefinder.data.topReposQuery
import com.example.githubissuefinder.model.IssueLabelFilter
import com.example.githubissuefinder.model.LanguageFilter
import com.example.githubissuefinder.testutil.MainDispatcherRule
import com.example.githubissuefinder.testutil.ViewModelFixtures
import com.example.githubissuefinder.testutil.issueDto
import com.example.githubissuefinder.testutil.repoGroupOf
import com.example.githubissuefinder.testutil.searchFailed
import com.example.githubissuefinder.testutil.searchOk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * TopReposViewModel against fakes: a real ViewModel over real
 * repositories whose storage and GitHub API are in-memory fakes, with
 * Dispatchers.Main replaced so its coroutines run immediately.
 */
class TopReposViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    @Before @After
    fun resetGlobalRateLimit() {
        RateLimitState.update(null)
    }

    private val gfi = IssueLabelFilter.GOOD_FIRST_ISSUE
    private val kotlin = LanguageFilter.KOTLIN
    private val limit = RateLimitInfo(remaining = 29, limit = 30, used = 1, resetsInLabel = "45s")

    private fun viewModel(f: ViewModelFixtures) = TopReposViewModel(
        f.modeRepository, f.tokenRepository, f.hiddenIssuesRepository,
        f.snippetRepository, f.cacheRepository, f.searchRepository,
    )

    private fun cached(f: ViewModelFixtures, entry: RepoIssueGroupCacheEntry) {
        f.cacheStorage.data = mapOf(cacheKeyFor(kotlin, gfi) to entry)
    }

    private fun entry(vararg groups: com.example.githubissuefinder.model.RepoIssueGroup, page: Int = 1, more: Boolean = false, at: Long = 777L) =
        RepoIssueGroupCacheEntry(groups.toList(), totalIssueCount = 5, currentPage = page, canLoadMore = more, fetchedAtMs = at)

    private fun fullPage(owner: String = "a", repo: String = "one", from: Long = 1) =
        (from until from + TOP_REPOS_PER_PAGE).map { issueDto(it, owner, repo) }

    // ---- loading: cache first ----

    @Test
    fun aCacheHitShowsTheCachedListWithoutSearching() {
        val f = ViewModelFixtures()
        val group = repoGroupOf("a", "one", 1, 2)
        cached(f, entry(group, page = 2, more = true, at = 777L))

        val vm = viewModel(f)

        val state = vm.uiState.value
        assertEquals(listOf(group), state.repoGroups)
        assertEquals(2, state.currentPage)
        assertTrue(state.canLoadMore)
        assertEquals(777L, state.lastUpdatedAtMs)
        assertFalse(state.isLoading)
        assertTrue(f.api.calls.isEmpty())
    }

    @Test
    fun aCacheHitFillsInTheRateLimitWhenItIsUnknown() {
        val f = ViewModelFixtures()
        f.api.rateLimit = limit
        cached(f, entry(repoGroupOf("a", "one", 1)))

        viewModel(f)

        assertEquals(limit, RateLimitState.info.value)
        assertEquals(1, f.api.rateLimitCalls)
        assertTrue(f.api.calls.isEmpty())
    }

    @Test
    fun aCacheMissSearchesThenShowsAndSavesTheResult() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ ->
            searchOk(listOf(issueDto(1, "a", "one"), issueDto(2, "a", "one"), issueDto(3, "b", "two")), total = 40, rateLimit = limit)
        }

        val vm = viewModel(f)

        val state = vm.uiState.value
        assertEquals(listOf("a/one", "b/two"), state.repoGroups.map { it.repoFullName })
        assertEquals(40, state.totalIssueCount)
        assertEquals(1, state.currentPage)
        assertFalse(state.canLoadMore)
        assertFalse(state.isLoading)
        assertEquals(Triple(topReposQuery(gfi, kotlin), TOP_REPOS_PER_PAGE, 1), f.api.calls.single())
        assertEquals(setOf(cacheKeyFor(kotlin, gfi)), f.cacheStorage.data.keys)
        assertEquals(limit, RateLimitState.info.value)
    }

    // ---- loading: problems ----

    @Test
    fun noTokenShowsTheNoTokenStateAndSearchesNothing() {
        val f = ViewModelFixtures(buildToken = "")

        val vm = viewModel(f)

        assertTrue(vm.uiState.value.noTokenConfigured)
        assertFalse(vm.uiState.value.isLoading)
        assertTrue(f.api.calls.isEmpty())
    }

    @Test
    fun aRejectedTokenIsFlaggedSoTheScreenCanSaySo() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> searchFailed(401) }

        val state = viewModel(f).uiState.value

        assertTrue(state.tokenRejected)
        assertEquals("GitHub returned an error (HTTP 401)", state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun anotherHttpErrorShowsItsCodeWithoutRejectingTheToken() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> searchFailed(500) }

        val state = viewModel(f).uiState.value

        assertFalse(state.tokenRejected)
        assertEquals("GitHub returned an error (HTTP 500)", state.errorMessage)
    }

    @Test
    fun aFailedRequestShowsWhatWentWrong() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> throw java.io.IOException("boom") }

        val state = viewModel(f).uiState.value

        assertEquals("Couldn't load issues: boom", state.errorMessage)
        assertFalse(state.isLoading)
    }

    // ---- Load More ----

    @Test
    fun aFullPageEnablesLoadMoreAndLoadMoreMergesTheNextPage() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, page ->
            if (page == 1) searchOk(fullPage("a", "one", 1), total = 200) else searchOk(listOf(issueDto(500, "b", "two")), total = 200)
        }
        val vm = viewModel(f)
        assertTrue(vm.uiState.value.canLoadMore)

        vm.loadMore()

        val state = vm.uiState.value
        assertEquals(2, state.currentPage)
        assertEquals(listOf("a/one", "b/two"), state.repoGroups.map { it.repoFullName })
        assertFalse(state.canLoadMore)
        assertEquals(2, f.api.calls.last().third)
    }

    @Test
    fun loadMoreHittingTheRateLimitTellsYouAndKeepsLoadMoreAvailable() = runBlocking {
        val f = ViewModelFixtures()
        val exhausted = RateLimitInfo(remaining = 0, limit = 30, used = 30, resetsInLabel = "40s")
        f.api.handler = { _, _, page -> if (page == 1) searchOk(fullPage(), total = 200) else searchFailed(403, exhausted) }
        val vm = viewModel(f)

        vm.loadMore()

        val state = vm.uiState.value
        assertTrue(state.canLoadMore)
        assertEquals(1, state.currentPage)
        assertNull(state.loadMoreError)
        assertFalse(state.isLoadingMore)
        assertEquals("GitHub's search rate limit was reached — try again shortly", vm.snackbarEvents.first())
    }

    @Test
    fun aFailedLoadMoreShowsItsErrorInTheFooter() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, page -> if (page == 1) searchOk(fullPage(), total = 200) else searchFailed(500) }
        val vm = viewModel(f)

        vm.loadMore()

        assertEquals("GitHub returned an error (HTTP 500)", vm.uiState.value.loadMoreError)
        assertFalse(vm.uiState.value.isLoadingMore)
    }

    // ---- switching ----

    @Test
    fun choosingTheOtherModeSwitchesAndReloads() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1))) }
        val vm = viewModel(f)

        vm.onModeSelected(IssueLabelFilter.HELP_WANTED)

        assertEquals(IssueLabelFilter.HELP_WANTED, f.modeRepository.mode.value)
        assertEquals(topReposQuery(IssueLabelFilter.HELP_WANTED, kotlin), f.api.calls.last().first)
    }

    @Test
    fun choosingTheModeThatIsAlreadyActiveDoesNothing() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1))) }
        val vm = viewModel(f)
        val callsBefore = f.api.calls.size

        vm.onModeSelected(gfi)

        assertEquals(callsBefore, f.api.calls.size)
    }

    @Test
    fun choosingAnotherLanguageReloadsForIt() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1))) }
        val vm = viewModel(f)

        vm.onLanguageSelected(LanguageFilter.JAVA)

        assertEquals(LanguageFilter.JAVA, vm.uiState.value.selectedLanguage)
        assertEquals(topReposQuery(gfi, LanguageFilter.JAVA), f.api.calls.last().first)
    }

    @Test
    fun clearingTheCacheEmptiesItAndLoadsFreshData() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1))) }
        val vm = viewModel(f)
        assertEquals(1, f.api.calls.size)

        vm.clearCache()

        // Had the cache survived, the reload would have been a cache hit with no new search.
        assertEquals(2, f.api.calls.size)
    }

    // ---- token changes ----

    @Test
    fun savingATokenWhileThereWasNoneLoadsTheList() {
        val f = ViewModelFixtures(buildToken = "")
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1))) }
        val vm = viewModel(f)
        assertTrue(vm.uiState.value.noTokenConfigured)

        f.tokenRepository.setUserToken("my-token")

        assertFalse(vm.uiState.value.noTokenConfigured)
        assertEquals(1, vm.uiState.value.repoGroups.size)
    }

    @Test
    fun savingANewTokenAfterARejectionClearsItAndReloads() {
        val f = ViewModelFixtures()
        var rejecting = true
        f.api.handler = { _, _, _ -> if (rejecting) searchFailed(401) else searchOk(listOf(issueDto(1))) }
        val vm = viewModel(f)
        assertTrue(vm.uiState.value.tokenRejected)
        rejecting = false

        f.tokenRepository.setUserToken("fresh-token")

        assertFalse(vm.uiState.value.tokenRejected)
        assertNull(vm.uiState.value.errorMessage)
        assertEquals(1, vm.uiState.value.repoGroups.size)
    }

    @Test
    fun changingTheTokenWhileTheListWorksDoesNotReload() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1))) }
        viewModel(f)
        val callsBefore = f.api.calls.size

        f.tokenRepository.setUserToken("another-token")

        assertEquals(callsBefore, f.api.calls.size)
    }

    @Test
    fun hasTokenFollowsTheTokenRepository() {
        val f = ViewModelFixtures(buildToken = "")
        val vm = viewModel(f)
        assertFalse(vm.hasToken.value)

        f.tokenRepository.setUserToken("x")

        assertTrue(vm.hasToken.value)
    }

    // ---- Check for New Repos / write-back ----

    @Test
    fun checkingForNewReposMergesWhatIsNewAndSaysSo() = runBlocking {
        val f = ViewModelFixtures()
        cached(f, entry(repoGroupOf("a", "one", 1), page = 1, more = true))
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1, "a", "one"), issueDto(2, "b", "two")), total = 9) }
        val vm = viewModel(f)

        vm.checkForNewRepos()

        assertEquals("1 new repo, 1 new issue", vm.snackbarEvents.first())
        assertEquals(setOf("a/one", "b/two"), vm.uiState.value.repoGroups.map { it.repoFullName }.toSet())
        assertEquals(9, vm.uiState.value.totalIssueCount)
        // Load More's progress is left exactly where it was.
        assertEquals(1, vm.uiState.value.currentPage)
        assertTrue(vm.uiState.value.canLoadMore)
        assertEquals(2, f.cacheStorage.data.getValue(cacheKeyFor(kotlin, gfi)).repoGroups.size)
    }

    @Test
    fun checkingForNewReposWhenNothingChangedSaysNothingNew() = runBlocking {
        val f = ViewModelFixtures()
        cached(f, entry(repoGroupOf("a", "one", 1)))
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1, "a", "one"))) }
        val vm = viewModel(f)

        vm.checkForNewRepos()

        assertTrue(vm.snackbarEvents.first().startsWith("Nothing new since"))
    }

    @Test
    fun checkingForNewReposThatFailsSaysSo() = runBlocking {
        val f = ViewModelFixtures()
        cached(f, entry(repoGroupOf("a", "one", 1)))
        f.api.handler = { _, _, _ -> searchFailed(500) }
        val vm = viewModel(f)

        vm.checkForNewRepos()

        assertEquals("Couldn't check for new repos (HTTP 500)", vm.snackbarEvents.first())
    }

    @Test
    fun aRepoRefreshedElsewhereIsAdoptedFromTheCache() = runBlocking {
        val f = ViewModelFixtures()
        cached(f, entry(repoGroupOf("a", "one", 1, 2)))
        val vm = viewModel(f)
        assertEquals(2, vm.uiState.value.repoGroups.single().matchCount)

        f.cacheRepository.replaceRepoGroup(kotlin, gfi, "a/one", repoGroupOf("a", "one", 1))
        vm.syncFromCache()

        assertEquals(1, vm.uiState.value.repoGroups.single().matchCount)
    }

    // ---- delegations ----

    @Test
    fun aRepoCardAppearingLoadsItsReadmePreview() {
        val f = ViewModelFixtures(readmeSource = com.example.githubissuefinder.testutil.FakeReadmeSource(ReadmeFetch.Resolved("# Hello\n\nWorld")))
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
}
