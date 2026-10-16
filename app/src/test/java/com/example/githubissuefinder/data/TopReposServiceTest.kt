package com.example.githubissuefinder.data

import com.example.githubissuefinder.model.IssueLabelFilter
import com.example.githubissuefinder.model.LanguageFilter
import com.example.githubissuefinder.model.RepoIssueGroup
import com.example.githubissuefinder.testutil.ViewModelFixtures
import com.example.githubissuefinder.testutil.issueDto
import com.example.githubissuefinder.testutil.repoGroupOf
import com.example.githubissuefinder.testutil.searchFailed
import com.example.githubissuefinder.testutil.searchOk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TopReposService against fakes: the real service over real repositories
 * whose storage and GitHub API are in-memory fakes, doing its work
 * immediately on the calling thread. These cover everything that decides
 * what the Top Repos list holds; the wording the user reads is covered
 * in TopReposUiMappingTest and TopReposViewModelTest.
 */
class TopReposServiceTest {

    private val gfi = IssueLabelFilter.GOOD_FIRST_ISSUE
    private val kotlin = LanguageFilter.KOTLIN
    private val limit = RateLimitInfo(remaining = 29, limit = 30, used = 1, resetsInLabel = "45s")

    /** The service from [f], opened the way constructing the ViewModel opens it. */
    private fun opened(f: ViewModelFixtures): TopReposService = f.topReposService.also { it.open() }

    private fun cached(f: ViewModelFixtures, entry: RepoIssueGroupCacheEntry) {
        f.cacheStorage.data = mapOf(cacheKeyFor(kotlin, gfi) to entry)
    }

    private fun entry(vararg groups: RepoIssueGroup, page: Int = 1, more: Boolean = false, at: Long = 777L) =
        RepoIssueGroupCacheEntry(groups.toList(), totalIssueCount = 5, currentPage = page, canLoadMore = more, fetchedAtMs = at)

    private fun fullPage(owner: String = "a", repo: String = "one", from: Long = 1) =
        (from until from + TOP_REPOS_PER_PAGE).map { issueDto(it, owner, repo) }

    // ---- loading: cache first ----

    @Test
    fun aCacheHitHoldsTheCachedListWithoutSearching() {
        val f = ViewModelFixtures()
        val group = repoGroupOf("a", "one", 1, 2)
        cached(f, entry(group, page = 2, more = true, at = 777L))

        val state = opened(f).state.value

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

        opened(f)

        assertEquals(limit, f.searchRepository.rateLimit.value)
        assertEquals(1, f.api.rateLimitCalls)
        assertTrue(f.api.calls.isEmpty())
    }

    @Test
    fun aCacheMissSearchesThenHoldsAndSavesTheResult() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ ->
            searchOk(listOf(issueDto(1, "a", "one"), issueDto(2, "a", "one"), issueDto(3, "b", "two")), total = 40, rateLimit = limit)
        }

        val state = opened(f).state.value

        assertEquals(listOf("a/one", "b/two"), state.repoGroups.map { it.repoFullName })
        assertEquals(40, state.totalIssueCount)
        assertEquals(1, state.currentPage)
        assertFalse(state.canLoadMore)
        assertFalse(state.isLoading)
        assertEquals(Triple(topReposQuery(gfi, kotlin), TOP_REPOS_PER_PAGE, 1), f.api.calls.single())
        assertEquals(setOf(cacheKeyFor(kotlin, gfi)), f.cacheStorage.data.keys)
        assertEquals(limit, f.searchRepository.rateLimit.value)
    }

    // ---- loading: problems ----

    @Test
    fun noTokenHoldsTheNoTokenStateAndSearchesNothing() {
        val f = ViewModelFixtures(buildToken = "")

        val state = opened(f).state.value

        assertTrue(state.noTokenConfigured)
        assertFalse(state.isLoading)
        assertTrue(f.api.calls.isEmpty())
    }

    @Test
    fun aRejectedTokenIsFlagged() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> searchFailed(401) }

        val state = opened(f).state.value

        assertTrue(state.tokenRejected)
        assertEquals(TopReposFailure.Http(401), state.loadFailure)
        assertFalse(state.isLoading)
    }

    @Test
    fun anotherHttpErrorKeepsItsCodeWithoutRejectingTheToken() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> searchFailed(500) }

        val state = opened(f).state.value

        assertFalse(state.tokenRejected)
        assertEquals(TopReposFailure.Http(500), state.loadFailure)
    }

    @Test
    fun aFailedRequestKeepsWhatWentWrong() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> throw java.io.IOException("boom") }

        val state = opened(f).state.value

        assertEquals(TopReposFailure.Failed("boom"), state.loadFailure)
        assertFalse(state.isLoading)
    }

    // ---- Load More ----

    @Test
    fun aFullPageEnablesLoadMoreAndLoadMoreAddsTheNextPage() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, page ->
            if (page == 1) searchOk(fullPage("a", "one", 1), total = 200) else searchOk(listOf(issueDto(500, "b", "two")), total = 200)
        }
        val service = opened(f)
        assertTrue(service.state.value.canLoadMore)

        service.loadMore()

        val state = service.state.value
        assertEquals(2, state.currentPage)
        assertEquals(listOf("a/one", "b/two"), state.repoGroups.map { it.repoFullName })
        assertFalse(state.canLoadMore)
        assertEquals(2, f.api.calls.last().third)
    }

    @Test
    fun loadMoreHittingTheRateLimitReportsItAndKeepsLoadMoreAvailable() = runBlocking {
        val f = ViewModelFixtures()
        val exhausted = RateLimitInfo(remaining = 0, limit = 30, used = 30, resetsInLabel = "40s")
        f.api.handler = { _, _, page -> if (page == 1) searchOk(fullPage(), total = 200) else searchFailed(403, exhausted) }
        val service = opened(f)

        service.loadMore()

        val state = service.state.value
        assertTrue(state.canLoadMore)
        assertEquals(1, state.currentPage)
        assertNull(state.loadMoreFailure)
        assertFalse(state.isLoadingMore)
        assertEquals(TopReposEvent.RateLimited, service.events.first())
    }

    @Test
    fun aFailedLoadMoreKeepsItsFailureForTheFooter() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, page -> if (page == 1) searchOk(fullPage(), total = 200) else searchFailed(500) }
        val service = opened(f)

        service.loadMore()

        assertEquals(TopReposFailure.Http(500), service.state.value.loadMoreFailure)
        assertFalse(service.state.value.isLoadingMore)
    }

    // ---- switching ----

    @Test
    fun choosingTheOtherModeSwitchesAndReloads() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1))) }
        val service = opened(f)

        service.selectMode(IssueLabelFilter.HELP_WANTED)

        assertEquals(IssueLabelFilter.HELP_WANTED, f.modeRepository.mode.value)
        assertEquals(topReposQuery(IssueLabelFilter.HELP_WANTED, kotlin), f.api.calls.last().first)
    }

    @Test
    fun choosingTheModeThatIsAlreadyActiveDoesNothing() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1))) }
        val service = opened(f)
        val callsBefore = f.api.calls.size

        service.selectMode(gfi)

        assertEquals(callsBefore, f.api.calls.size)
    }

    @Test
    fun choosingAnotherLanguageReloadsForIt() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1))) }
        val service = opened(f)

        service.selectLanguage(LanguageFilter.JAVA)

        assertEquals(LanguageFilter.JAVA, service.state.value.selectedLanguage)
        assertEquals(topReposQuery(gfi, LanguageFilter.JAVA), f.api.calls.last().first)
    }

    @Test
    fun clearingTheCacheEmptiesItAndLoadsFreshData() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1))) }
        val service = opened(f)
        assertEquals(1, f.api.calls.size)

        service.clearCache()

        // Had the cache survived, the reload would have been a cache hit with no new search.
        assertEquals(2, f.api.calls.size)
    }

    // ---- token changes ----

    @Test
    fun savingATokenWhileThereWasNoneLoadsTheList() {
        val f = ViewModelFixtures(buildToken = "")
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1))) }
        val service = opened(f)
        assertTrue(service.state.value.noTokenConfigured)

        f.tokenRepository.setUserToken("my-token")

        assertFalse(service.state.value.noTokenConfigured)
        assertEquals(1, service.state.value.repoGroups.size)
    }

    @Test
    fun savingANewTokenAfterARejectionClearsItAndReloads() {
        val f = ViewModelFixtures()
        var rejecting = true
        f.api.handler = { _, _, _ -> if (rejecting) searchFailed(401) else searchOk(listOf(issueDto(1))) }
        val service = opened(f)
        assertTrue(service.state.value.tokenRejected)
        rejecting = false

        f.tokenRepository.setUserToken("fresh-token")

        assertFalse(service.state.value.tokenRejected)
        assertNull(service.state.value.loadFailure)
        assertEquals(1, service.state.value.repoGroups.size)
    }

    @Test
    fun changingTheTokenWhileTheListWorksDoesNotReload() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1))) }
        opened(f)
        val callsBefore = f.api.calls.size

        f.tokenRepository.setUserToken("another-token")

        assertEquals(callsBefore, f.api.calls.size)
    }

    @Test
    fun changingTheTokenReplacesTheOldRateLimitWithTheNewTokensNumbers() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1)), rateLimit = limit) }
        val service = opened(f)
        assertEquals(limit, service.rateLimit.value)
        val newTokensLimit = RateLimitInfo(remaining = 5, limit = 30, used = 25, resetsInLabel = "10s")
        f.api.rateLimit = newTokensLimit

        f.tokenRepository.setUserToken("another-token")

        assertEquals(newTokensLimit, service.rateLimit.value)
    }

    @Test
    fun hasTokenFollowsTheTokenRepository() {
        val f = ViewModelFixtures(buildToken = "")
        val service = opened(f)
        assertFalse(service.hasToken.value)

        f.tokenRepository.setUserToken("x")

        assertTrue(service.hasToken.value)
    }

    // ---- Check for New Repos / write-back ----

    @Test
    fun checkingForNewReposAddsWhatIsNewAndReportsTheCounts() = runBlocking {
        val f = ViewModelFixtures()
        cached(f, entry(repoGroupOf("a", "one", 1), page = 1, more = true, at = 777L))
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1, "a", "one"), issueDto(2, "b", "two")), total = 9) }
        val service = opened(f)

        service.checkForNewRepos()

        val event = service.events.first() as TopReposEvent.NewReposChecked
        assertEquals(1, event.newRepoCount)
        assertEquals(1, event.newIssueCount)
        assertEquals(777L, event.previousUpdatedAtMs)
        val state = service.state.value
        assertEquals(setOf("a/one", "b/two"), state.repoGroups.map { it.repoFullName }.toSet())
        assertEquals(9, state.totalIssueCount)
        // Load More's progress is left exactly where it was.
        assertEquals(1, state.currentPage)
        assertTrue(state.canLoadMore)
        assertEquals(2, f.cacheStorage.data.getValue(cacheKeyFor(kotlin, gfi)).repoGroups.size)
    }

    @Test
    fun checkingForNewReposWhenNothingChangedReportsZeroNew() = runBlocking {
        val f = ViewModelFixtures()
        cached(f, entry(repoGroupOf("a", "one", 1)))
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1, "a", "one"))) }
        val service = opened(f)

        service.checkForNewRepos()

        val event = service.events.first() as TopReposEvent.NewReposChecked
        assertEquals(0, event.newRepoCount)
        assertEquals(0, event.newIssueCount)
    }

    @Test
    fun checkingForNewReposThatFailsReportsTheStatus() = runBlocking {
        val f = ViewModelFixtures()
        cached(f, entry(repoGroupOf("a", "one", 1)))
        f.api.handler = { _, _, _ -> searchFailed(500) }
        val service = opened(f)

        service.checkForNewRepos()

        assertEquals(TopReposEvent.CheckFailedHttp(500), service.events.first())
    }

    @Test
    fun aRepoRefreshedElsewhereIsAdoptedFromTheCache() = runBlocking {
        val f = ViewModelFixtures()
        cached(f, entry(repoGroupOf("a", "one", 1, 2)))
        val service = opened(f)
        assertEquals(2, service.state.value.repoGroups.single().matchCount)

        f.cacheRepository.replaceRepoGroup(kotlin, gfi, "a/one", repoGroupOf("a", "one", 1))
        service.syncFromCache()

        assertEquals(1, service.state.value.repoGroups.single().matchCount)
    }

    // ---- visits ----

    @Test
    fun openingAgainStartsAFreshVisitInTheDefaultLanguage() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1))) }
        val service = opened(f)
        service.selectLanguage(LanguageFilter.JAVA)

        service.open()

        assertEquals(kotlin, service.state.value.selectedLanguage)
        assertFalse(service.state.value.isLoading)
    }

    @Test
    fun openingAgainDropsEventsNobodyCollected() = runBlocking {
        val f = ViewModelFixtures()
        cached(f, entry(repoGroupOf("a", "one", 1)))
        f.api.handler = { _, _, _ -> searchFailed(500) }
        val service = opened(f)
        service.checkForNewRepos()

        service.open()

        assertNull(withTimeoutOrNull(50) { service.events.first() })
    }
}
