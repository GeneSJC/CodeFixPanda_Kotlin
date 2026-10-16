package com.example.githubissuefinder.data

import com.example.githubissuefinder.model.IssueLabelFilter
import com.example.githubissuefinder.model.LanguageFilter
import com.example.githubissuefinder.network.dto.IssueDto
import com.example.githubissuefinder.network.dto.IssueSearchResponseDto
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IssueSearchRepositoryTest {

    private class FakeApi(
        private val search: () -> IssueSearchApiResponse = { error("search not expected") },
        private val rateLimit: RateLimitInfo? = null,
    ) : IssueSearchApi {
        val calls = mutableListOf<Triple<String, Int, Int>>()
        var rateLimitCalls = 0
        override suspend fun searchIssues(query: String, perPage: Int, page: Int): IssueSearchApiResponse {
            calls += Triple(query, perPage, page)
            return search()
        }
        override suspend fun fetchRateLimit(): RateLimitInfo? {
            rateLimitCalls++
            return rateLimit
        }
    }

    private val gfi = IssueLabelFilter.GOOD_FIRST_ISSUE
    private val kotlin = LanguageFilter.KOTLIN
    private val limit = RateLimitInfo(remaining = 29, limit = 30, used = 1, resetsInLabel = "45s")

    private fun dto(id: Long) = IssueDto(
        id = id, number = 1, title = "Issue $id", body = "body", state = "open",
        htmlUrl = "https://github.com/octo/repo/issues/$id",
        repositoryUrl = "https://api.github.com/repos/octo/repo",
        comments = 2, updatedAt = "2026-10-01T12:00:00Z",
    )

    private fun ok(vararg ids: Long, total: Int = 99, rateLimit: RateLimitInfo? = limit) = IssueSearchApiResponse(
        code = 200, isSuccessful = true,
        body = IssueSearchResponseDto(totalCount = total, items = ids.map(::dto)),
        rateLimit = rateLimit,
    )

    private fun failed(code: Int, rateLimit: RateLimitInfo? = null) =
        IssueSearchApiResponse(code = code, isSuccessful = false, body = null, rateLimit = rateLimit)

    private fun repository(api: IssueSearchApi, hasToken: Boolean = true) = IssueSearchRepository(api, { hasToken })

    @Before @After
    fun resetGlobalRateLimit() {
        RateLimitState.update(null)
    }

    // ---- query builders ----

    @Test
    fun topReposQueryCombinesLabelOpenStateAndLanguage() {
        assertEquals("label:\"good first issue\" state:open language:kotlin", topReposQuery(gfi, kotlin))
    }

    @Test
    fun topReposQueryOmitsTheLanguageForAny() {
        assertEquals("label:\"help wanted\" state:open", topReposQuery(IssueLabelFilter.HELP_WANTED, LanguageFilter.ANY))
    }

    @Test
    fun repoIssuesQueryScopesToOneRepo() {
        assertEquals("repo:octo/repo label:\"good first issue\" state:open", repoIssuesQuery("octo/repo", gfi))
    }

    // ---- searching ----

    @Test
    fun noTokenMeansNoRequestIsMade() = runBlocking {
        val api = FakeApi()
        assertEquals(IssueSearchResult.NoToken, repository(api, hasToken = false).searchTopRepos(gfi, kotlin, 1))
        assertEquals(IssueSearchResult.NoToken, repository(api, hasToken = false).searchRepoIssues("octo/repo", gfi, kotlin))
        assertTrue(api.calls.isEmpty())
    }

    @Test
    fun topReposAsksForThePageWithTheTopReposPageSize() = runBlocking {
        val api = FakeApi(search = { ok(1) })

        repository(api).searchTopRepos(gfi, kotlin, page = 3)

        assertEquals(Triple(topReposQuery(gfi, kotlin), TOP_REPOS_PER_PAGE, 3), api.calls.single())
    }

    @Test
    fun repoIssuesAsksForTheFirstPageWithTheMaxPageSize() = runBlocking {
        val api = FakeApi(search = { ok(1) })

        repository(api).searchRepoIssues("octo/repo", gfi, kotlin)

        assertEquals(Triple(repoIssuesQuery("octo/repo", gfi), REPO_ISSUES_MAX_PER_PAGE, 1), api.calls.single())
    }

    @Test
    fun aSuccessfulSearchReturnsTheIssuesAndTotalConvertedToTheAppType() = runBlocking {
        val result = repository(FakeApi(search = { ok(1, 2, total = 57) })).searchTopRepos(gfi, kotlin, 1)

        val page = (result as IssueSearchResult.Success).page
        assertEquals(57, page.totalCount)
        assertEquals(listOf(1L, 2L), page.issues.map { it.id })
        assertEquals("octo/repo", page.issues.first().repoFullName)
        assertEquals("kotlin", page.issues.first().language)
    }

    @Test
    fun aSuccessfulSearchUpdatesTheRateLimitState() = runBlocking {
        repository(FakeApi(search = { ok(1, rateLimit = limit) })).searchTopRepos(gfi, kotlin, 1)
        assertEquals(limit, RateLimitState.info.value)
    }

    @Test
    fun aFailedSearchDoesNotUpdateTheRateLimitState() = runBlocking {
        RateLimitState.update(limit)

        repository(FakeApi(search = { failed(500, rateLimit = RateLimitInfo(5, 30, 25, "10s")) })).searchTopRepos(gfi, kotlin, 1)

        assertEquals(limit, RateLimitState.info.value)
    }

    @Test
    fun aServerErrorIsAnHttpErrorThatIsNotRateLimited() = runBlocking {
        val result = repository(FakeApi(search = { failed(500) })).searchTopRepos(gfi, kotlin, 1)
        assertEquals(IssueSearchResult.HttpError(500, rateLimited = false), result)
    }

    @Test
    fun aFourOhThreeWithNothingLeftIsRateLimited() = runBlocking {
        val exhausted = RateLimitInfo(remaining = 0, limit = 30, used = 30, resetsInLabel = "40s")
        val result = repository(FakeApi(search = { failed(403, exhausted) })).searchTopRepos(gfi, kotlin, 1)
        assertEquals(IssueSearchResult.HttpError(403, rateLimited = true), result)
    }

    @Test
    fun aFourOhThreeWithQuotaLeftIsNotRateLimited() = runBlocking {
        val result = repository(FakeApi(search = { failed(403, limit) })).searchTopRepos(gfi, kotlin, 1)
        assertEquals(IssueSearchResult.HttpError(403, rateLimited = false), result)
    }

    @Test
    fun aFourOhThreeWithNoRateLimitHeadersIsNotRateLimited() = runBlocking {
        val result = repository(FakeApi(search = { failed(403, null) })).searchTopRepos(gfi, kotlin, 1)
        assertEquals(IssueSearchResult.HttpError(403, rateLimited = false), result)
    }

    @Test
    fun aFourOhOneIsAnHttpErrorSoTheCallerCanTreatItAsARejectedToken() = runBlocking {
        val result = repository(FakeApi(search = { failed(401) })).searchTopRepos(gfi, kotlin, 1)
        assertEquals(IssueSearchResult.HttpError(401, rateLimited = false), result)
    }

    @Test
    fun aSuccessfulResponseWithNoBodyIsAnHttpError() = runBlocking {
        val noBody = IssueSearchApiResponse(code = 200, isSuccessful = true, body = null, rateLimit = limit)
        val result = repository(FakeApi(search = { noBody })).searchTopRepos(gfi, kotlin, 1)
        assertEquals(IssueSearchResult.HttpError(200, rateLimited = false), result)
    }

    @Test
    fun aRequestThatThrowsIsFailedWithItsMessage() = runBlocking {
        val result = repository(FakeApi(search = { throw java.io.IOException("timed out") })).searchTopRepos(gfi, kotlin, 1)
        assertEquals(IssueSearchResult.Failed("timed out"), result)
    }

    // ---- rate limit lookup ----

    @Test
    fun theRateLimitIsLookedUpWhenUnknown() = runBlocking {
        val api = FakeApi(rateLimit = limit)

        repository(api).loadRateLimitIfUnknown()

        assertEquals(limit, RateLimitState.info.value)
        assertEquals(1, api.rateLimitCalls)
    }

    @Test
    fun theRateLimitIsNotLookedUpWhenAlreadyKnown() = runBlocking {
        RateLimitState.update(limit)
        val api = FakeApi(rateLimit = RateLimitInfo(1, 30, 29, "5s"))

        repository(api).loadRateLimitIfUnknown()

        assertEquals(limit, RateLimitState.info.value)
        assertEquals(0, api.rateLimitCalls)
    }

    @Test
    fun theRateLimitIsNotLookedUpWithoutAToken() = runBlocking {
        val api = FakeApi(rateLimit = limit)

        repository(api, hasToken = false).loadRateLimitIfUnknown()

        assertNull(RateLimitState.info.value)
        assertEquals(0, api.rateLimitCalls)
    }

    @Test
    fun aFailedRateLimitLookupLeavesItUnknown() = runBlocking {
        repository(FakeApi(rateLimit = null)).loadRateLimitIfUnknown()
        assertFalse(RateLimitState.info.value != null)
    }
}
