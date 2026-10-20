package io.github.genesjc.githubissuefinder.data

import io.github.genesjc.githubissuefinder.model.IssueLabelFilter
import io.github.genesjc.githubissuefinder.model.LanguageFilter
import io.github.genesjc.githubissuefinder.model.RepoIssueGroup
import io.github.genesjc.githubissuefinder.testutil.ViewModelFixtures
import io.github.genesjc.githubissuefinder.testutil.issueDto
import io.github.genesjc.githubissuefinder.testutil.repoGroupOf
import io.github.genesjc.githubissuefinder.testutil.searchFailed
import io.github.genesjc.githubissuefinder.testutil.searchOk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TopReposStateHolder against fakes: the real state holder over real repositories
 * whose storage and GitHub API are in-memory fakes, doing its work
 * immediately on the calling thread. These cover everything that decides
 * what the Top Repos list holds; the wording the user reads is covered
 * in TopReposUiMappingTest and TopReposViewModelTest.
 */
class TopReposStateHolderTest {

    private val gfi = IssueLabelFilter.GOOD_FIRST_ISSUE
    private val kotlin = LanguageFilter.KOTLIN
    private val limit = RateLimitInfo(remaining = 29, limit = 30, used = 1, resetsInSeconds = 45L)

    /** The state holder from [f], opened the way constructing the ViewModel opens it. */
    private fun opened(f: ViewModelFixtures): TopReposStateHolder = f.topReposStateHolder.also { it.open() }

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
    fun aRejectedTokenIsFlagged() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> searchFailed(401) }

        val state = opened(f).state.value

        assertTrue(state.tokenRejected)
        assertEquals(SearchFailure.Http(401), state.loadFailure)
        assertFalse(state.isLoading)
    }

    @Test
    fun anotherHttpErrorKeepsItsCodeWithoutRejectingTheToken() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> searchFailed(500) }

        val state = opened(f).state.value

        assertFalse(state.tokenRejected)
        assertEquals(SearchFailure.Http(500), state.loadFailure)
    }

    @Test
    fun aFailedRequestKeepsWhatWentWrong() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> throw java.io.IOException("boom") }

        val state = opened(f).state.value

        assertEquals(SearchFailure.Failed("boom"), state.loadFailure)
        assertFalse(state.isLoading)
    }

    // ---- Load More ----

    @Test
    fun aFullPageEnablesLoadMoreAndLoadMoreAddsTheNextPage() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, page ->
            if (page == 1) searchOk(fullPage("a", "one", 1), total = 200) else searchOk(listOf(issueDto(500, "b", "two")), total = 200)
        }
        val stateHolder = opened(f)
        assertTrue(stateHolder.state.value.canLoadMore)

        stateHolder.loadMore()

        val state = stateHolder.state.value
        assertEquals(2, state.currentPage)
        assertEquals(listOf("a/one", "b/two"), state.repoGroups.map { it.repoFullName })
        assertFalse(state.canLoadMore)
        assertEquals(2, f.api.calls.last().third)
    }

    @Test
    fun loadMoreHittingTheRateLimitReportsItAndKeepsLoadMoreAvailable() = runBlocking {
        val f = ViewModelFixtures()
        val exhausted = RateLimitInfo(remaining = 0, limit = 30, used = 30, resetsInSeconds = 40L)
        f.api.handler = { _, _, page -> if (page == 1) searchOk(fullPage(), total = 200) else searchFailed(403, exhausted) }
        val stateHolder = opened(f)

        stateHolder.loadMore()

        val state = stateHolder.state.value
        assertTrue(state.canLoadMore)
        assertEquals(1, state.currentPage)
        assertNull(state.loadMoreFailure)
        assertFalse(state.isLoadingMore)
        assertEquals(TopReposEvent.RateLimited, stateHolder.events.first())
    }

    @Test
    fun aFailedLoadMoreKeepsItsFailureForTheFooter() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, page -> if (page == 1) searchOk(fullPage(), total = 200) else searchFailed(500) }
        val stateHolder = opened(f)

        stateHolder.loadMore()

        assertEquals(SearchFailure.Http(500), stateHolder.state.value.loadMoreFailure)
        assertFalse(stateHolder.state.value.isLoadingMore)
    }

    // ---- switching ----

    @Test
    fun choosingTheOtherModeSwitchesAndReloads() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1))) }
        val stateHolder = opened(f)

        stateHolder.selectMode(IssueLabelFilter.HELP_WANTED)

        assertEquals(IssueLabelFilter.HELP_WANTED, f.modeRepository.mode.value)
        assertEquals(topReposQuery(IssueLabelFilter.HELP_WANTED, kotlin), f.api.calls.last().first)
    }

    @Test
    fun choosingTheModeThatIsAlreadyActiveDoesNothing() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1))) }
        val stateHolder = opened(f)
        val callsBefore = f.api.calls.size

        stateHolder.selectMode(gfi)

        assertEquals(callsBefore, f.api.calls.size)
    }

    @Test
    fun choosingAnotherLanguageReloadsForIt() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1))) }
        val stateHolder = opened(f)

        stateHolder.selectLanguage(LanguageFilter.JAVA)

        assertEquals(LanguageFilter.JAVA, stateHolder.state.value.selectedLanguage)
        assertEquals(topReposQuery(gfi, LanguageFilter.JAVA), f.api.calls.last().first)
    }

    @Test
    fun clearingTheCacheEmptiesItAndLoadsFreshData() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1))) }
        val stateHolder = opened(f)
        assertEquals(1, f.api.calls.size)

        stateHolder.clearCache()

        // Had the cache survived, the reload would have been a cache hit with no new search.
        assertEquals(2, f.api.calls.size)
    }

    // ---- token changes ----

    @Test
    fun savingATokenWhileThereWasNoneReloadsTheListFromTheDataNowInUse() {
        val f = ViewModelFixtures(buildToken = "")
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1))) }
        val stateHolder = opened(f)
        val callsBefore = f.api.calls.size
        // The cache for the data in use is separate (demo and real never share one), so the new data has none yet.
        f.cacheStorage.data = emptyMap()
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1), issueDto(2, "other", "repo"))) }

        f.tokenRepository.setUserToken("my-token")

        assertTrue(f.api.calls.size > callsBefore)
        assertEquals(2, stateHolder.state.value.repoGroups.size)
    }

    @Test
    fun savingANewTokenAfterARejectionClearsItAndReloads() {
        val f = ViewModelFixtures()
        var rejecting = true
        f.api.handler = { _, _, _ -> if (rejecting) searchFailed(401) else searchOk(listOf(issueDto(1))) }
        val stateHolder = opened(f)
        assertTrue(stateHolder.state.value.tokenRejected)
        rejecting = false

        f.tokenRepository.setUserToken("fresh-token")

        assertFalse(stateHolder.state.value.tokenRejected)
        assertNull(stateHolder.state.value.loadFailure)
        assertEquals(1, stateHolder.state.value.repoGroups.size)
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
        val stateHolder = opened(f)
        assertEquals(limit, stateHolder.rateLimit.value)
        val newTokensLimit = RateLimitInfo(remaining = 5, limit = 30, used = 25, resetsInSeconds = 10L)
        f.api.rateLimit = newTokensLimit

        f.tokenRepository.setUserToken("another-token")

        assertEquals(newTokensLimit, stateHolder.rateLimit.value)
    }

    @Test
    fun hasTokenFollowsTheTokenRepository() {
        val f = ViewModelFixtures(buildToken = "")
        val stateHolder = opened(f)
        assertFalse(stateHolder.hasToken.value)

        f.tokenRepository.setUserToken("x")

        assertTrue(stateHolder.hasToken.value)
    }

    // ---- Check for New Repos / write-back ----

    @Test
    fun checkingForNewReposAddsWhatIsNewAndReportsTheCounts() = runBlocking {
        val f = ViewModelFixtures()
        cached(f, entry(repoGroupOf("a", "one", 1), page = 1, more = true, at = 777L))
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1, "a", "one"), issueDto(2, "b", "two")), total = 9) }
        val stateHolder = opened(f)

        stateHolder.checkForNewRepos()

        val event = stateHolder.events.first() as TopReposEvent.NewReposChecked
        assertEquals(1, event.newRepoCount)
        assertEquals(1, event.newIssueCount)
        assertEquals(777L, event.previousUpdatedAtMs)
        val state = stateHolder.state.value
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
        val stateHolder = opened(f)

        stateHolder.checkForNewRepos()

        val event = stateHolder.events.first() as TopReposEvent.NewReposChecked
        assertEquals(0, event.newRepoCount)
        assertEquals(0, event.newIssueCount)
    }

    @Test
    fun checkingForNewReposThatFailsReportsTheStatus() = runBlocking {
        val f = ViewModelFixtures()
        cached(f, entry(repoGroupOf("a", "one", 1)))
        f.api.handler = { _, _, _ -> searchFailed(500) }
        val stateHolder = opened(f)

        stateHolder.checkForNewRepos()

        assertEquals(TopReposEvent.CheckFailedHttp(500), stateHolder.events.first())
    }

    @Test
    fun aRepoRefreshedElsewhereIsAdoptedFromTheCache() = runBlocking {
        val f = ViewModelFixtures()
        cached(f, entry(repoGroupOf("a", "one", 1, 2)))
        val stateHolder = opened(f)
        assertEquals(2, stateHolder.state.value.repoGroups.single().matchCount)

        f.cacheRepository.replaceRepoGroup(kotlin, gfi, "a/one", repoGroupOf("a", "one", 1))
        stateHolder.syncFromCache()

        assertEquals(1, stateHolder.state.value.repoGroups.single().matchCount)
    }

    // ---- visits ----

    @Test
    fun openingAgainStartsAFreshVisitInTheDefaultLanguage() {
        val f = ViewModelFixtures()
        f.api.handler = { _, _, _ -> searchOk(listOf(issueDto(1))) }
        val stateHolder = opened(f)
        stateHolder.selectLanguage(LanguageFilter.JAVA)

        stateHolder.open()

        assertEquals(kotlin, stateHolder.state.value.selectedLanguage)
        assertFalse(stateHolder.state.value.isLoading)
    }

    @Test
    fun openingAgainDropsEventsNobodyCollected() = runBlocking {
        val f = ViewModelFixtures()
        cached(f, entry(repoGroupOf("a", "one", 1)))
        f.api.handler = { _, _, _ -> searchFailed(500) }
        val stateHolder = opened(f)
        stateHolder.checkForNewRepos()

        stateHolder.open()

        assertNull(withTimeoutOrNull(50) { stateHolder.events.first() })
    }
}
