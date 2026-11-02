package io.github.genesjc.opensourcepanda.data

import io.github.genesjc.opensourcepanda.data.network.dto.IssueDto
import io.github.genesjc.opensourcepanda.data.network.dto.IssueSearchResponseDto
import io.github.genesjc.opensourcepanda.domain.REPO_ISSUES_MAX_PER_PAGE
import io.github.genesjc.opensourcepanda.domain.TOP_REPOS_PER_PAGE
import io.github.genesjc.opensourcepanda.model.IssueLabelFilter
import io.github.genesjc.opensourcepanda.model.LanguageFilter
import io.github.genesjc.opensourcepanda.model.RateLimitInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    private val limit = RateLimitInfo(remaining = 29, limit = 30, used = 1, resetsInSeconds = 45L)

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

    private fun repository(api: IssueSearchApi) = IssueSearchRepository(api)

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
    fun aSuccessfulSearchUpdatesTheRateLimit() = runBlocking {
        val repo = repository(FakeApi(search = { ok(1, rateLimit = limit) }))

        repo.searchTopRepos(gfi, kotlin, 1)

        assertEquals(limit, repo.rateLimit.value)
    }

    @Test
    fun aFailedSearchDoesNotUpdateTheRateLimit() = runBlocking {
        var failing = false
        val repo = repository(
            FakeApi(search = { if (failing) failed(500, rateLimit = RateLimitInfo(5, 30, 25, 10L)) else ok(1, rateLimit = limit) }),
        )
        repo.searchTopRepos(gfi, kotlin, 1)
        failing = true

        repo.searchTopRepos(gfi, kotlin, 1)

        assertEquals(limit, repo.rateLimit.value)
    }

    @Test
    fun aServerErrorIsAnHttpErrorThatIsNotRateLimited() = runBlocking {
        val result = repository(FakeApi(search = { failed(500) })).searchTopRepos(gfi, kotlin, 1)
        assertEquals(IssueSearchResult.HttpError(500, rateLimited = false), result)
    }

    @Test
    fun aFourOhThreeWithNothingLeftIsRateLimited() = runBlocking {
        val exhausted = RateLimitInfo(remaining = 0, limit = 30, used = 30, resetsInSeconds = 40L)
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

        val repo = repository(api)

        repo.loadRateLimitIfUnknown()

        assertEquals(limit, repo.rateLimit.value)
        assertEquals(1, api.rateLimitCalls)
    }

    @Test
    fun theRateLimitIsNotLookedUpWhenAlreadyKnown() = runBlocking {
        val api = FakeApi(search = { ok(1, rateLimit = limit) }, rateLimit = RateLimitInfo(1, 30, 29, 5L))
        val repo = repository(api)
        repo.searchTopRepos(gfi, kotlin, 1)

        repo.loadRateLimitIfUnknown()

        assertEquals(limit, repo.rateLimit.value)
        assertEquals(0, api.rateLimitCalls)
    }

    @Test
    fun aFailedRateLimitLookupLeavesItUnknown() = runBlocking {
        val repo = repository(FakeApi(rateLimit = null))

        repo.loadRateLimitIfUnknown()

        assertNull(repo.rateLimit.value)
    }

    @Test
    fun clearingTheRateLimitForgetsTheNumbers() = runBlocking {
        val repo = repository(FakeApi(search = { ok(1, rateLimit = limit) }))
        repo.searchTopRepos(gfi, kotlin, 1)

        repo.clearRateLimit()

        assertNull(repo.rateLimit.value)
    }
}
