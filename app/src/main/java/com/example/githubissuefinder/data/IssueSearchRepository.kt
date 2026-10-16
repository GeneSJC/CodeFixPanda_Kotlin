package com.example.githubissuefinder.data

import com.example.githubissuefinder.model.Issue
import com.example.githubissuefinder.model.IssueLabelFilter
import com.example.githubissuefinder.model.LanguageFilter
import com.example.githubissuefinder.network.GitHubApiClient
import com.example.githubissuefinder.network.dto.IssueSearchResponseDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Issues per page for Top Repos' list. GitHub's Search API is 1-indexed and returns at most this many per request. */
const val TOP_REPOS_PER_PAGE = 30

/** The most issues one search can return; a single repo's full refresh asks for this many at once. */
const val REPO_ISSUES_MAX_PER_PAGE = 100

/** One page of search results, already converted to the app's own Issue type. */
data class IssueSearchPage(val issues: List<Issue>, val totalCount: Int)

/**
 * Every way a search can end, so callers react to an outcome instead of
 * poking at an HTTP response themselves.
 */
sealed interface IssueSearchResult {
    data class Success(val page: IssueSearchPage) : IssueSearchResult

    /** No GitHub token is in use, so nothing was requested. */
    data object NoToken : IssueSearchResult

    /**
     * GitHub answered with an error. [rateLimited] is true for a 403 whose
     * rate-limit headers say nothing is left — GitHub's own signal for
     * "slow down," distinct from a 401, which means the token itself was
     * rejected (check [code]).
     */
    data class HttpError(val code: Int, val rateLimited: Boolean) : IssueSearchResult

    /** The request itself failed (no network, a timeout, ...). */
    data class Failed(val message: String?) : IssueSearchResult
}

/** What came back from the Search API: the HTTP status, the body if any, and the rate-limit headers it carried. */
data class IssueSearchApiResponse(
    val code: Int,
    val isSuccessful: Boolean,
    val body: IssueSearchResponseDto?,
    val rateLimit: RateLimitInfo?,
)

/**
 * The two GitHub calls this repository makes, split out so the
 * repository's own rules can be unit-tested against a plain fake with no
 * network.
 */
interface IssueSearchApi {
    suspend fun searchIssues(query: String, perPage: Int, page: Int): IssueSearchApiResponse

    /** The Search API's current rate-limit numbers from GitHub's free /rate_limit endpoint, or null if they couldn't be read. */
    suspend fun fetchRateLimit(): RateLimitInfo?
}

/** The real calls, run off the main thread through GitHubApiClient. */
class GitHubIssueSearchApi : IssueSearchApi {

    override suspend fun searchIssues(query: String, perPage: Int, page: Int): IssueSearchApiResponse =
        withContext(Dispatchers.IO) {
            val response = GitHubApiClient.api.searchIssues(query = query, perPage = perPage, page = page)
            IssueSearchApiResponse(
                code = response.code(),
                isSuccessful = response.isSuccessful,
                body = response.body(),
                rateLimit = parseRateLimitHeaders(response.headers()),
            )
        }

    override suspend fun fetchRateLimit(): RateLimitInfo? =
        withContext(Dispatchers.IO) {
            try {
                val response = GitHubApiClient.api.getRateLimit()
                val body = response.body()
                if (response.isSuccessful && body != null) body.toSearchRateLimitInfo() else null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        }
}

/** The Search API query for Top Repos: open issues carrying the mode's label, optionally in one language. */
fun topReposQuery(mode: IssueLabelFilter, language: LanguageFilter): String = buildString {
    append("label:\"${mode.queryValue}\"")
    append(" state:open")
    language.queryValue?.let { append(" language:$it") }
}

/** The Search API query for one repo's own issues screen: that repo's open issues carrying the mode's label. */
fun repoIssuesQuery(repoFullName: String, mode: IssueLabelFilter): String =
    "repo:$repoFullName label:\"${mode.queryValue}\" state:open"

/**
 * The single place the app searches GitHub for issues: Top Repos' list
 * (and its Load More and "Check for New Repos"), and a single repo's
 * refresh. Before this, the two ViewModels built their queries, called
 * the API, read the rate-limit headers and sorted out the error cases
 * separately — the same code in three places.
 *
 * Why a repository: it turns "an HTTP response" into an
 * [IssueSearchResult] the callers can simply switch on, keeps the rules
 * in one place (no token means no request; the rate-limit state is
 * refreshed from every successful search), and lets those rules be
 * unit-tested without a network.
 *
 * The rate-limit chip's numbers are updated here, from the headers of
 * every SUCCESSFUL search. A failed response doesn't update them (it is
 * only inspected to tell a rate-limit 403 apart from other errors).
 *
 * [hasToken] is a parameter so a test can control it.
 */
class IssueSearchRepository(
    private val api: IssueSearchApi,
    private val hasToken: () -> Boolean = { ApiTokenRepository.instance.effectiveToken.value.isNotBlank() },
) {

    /** One page of Top Repos' list for [mode] and [language]. [page] is 1-indexed. */
    suspend fun searchTopRepos(mode: IssueLabelFilter, language: LanguageFilter, page: Int): IssueSearchResult =
        search(topReposQuery(mode, language), TOP_REPOS_PER_PAGE, page, language)

    /** Every open matching issue of one repo (up to [REPO_ISSUES_MAX_PER_PAGE]), for its "Refresh this repo". */
    suspend fun searchRepoIssues(repoFullName: String, mode: IssueLabelFilter, language: LanguageFilter): IssueSearchResult =
        search(repoIssuesQuery(repoFullName, mode), REPO_ISSUES_MAX_PER_PAGE, 1, language)

    /**
     * Fills in the rate-limit numbers when none are known yet (a list
     * loaded from the cache never made a search that could supply them).
     * Free: it uses GitHub's /rate_limit endpoint, which doesn't count
     * against any limit. A failure is ignored on purpose — the chip just
     * stays blank, and the next search fills it in.
     */
    suspend fun loadRateLimitIfUnknown() {
        if (RateLimitState.info.value != null || !hasToken()) return
        val info = api.fetchRateLimit() ?: return
        if (RateLimitState.info.value == null) RateLimitState.update(info)
    }

    private suspend fun search(query: String, perPage: Int, page: Int, language: LanguageFilter): IssueSearchResult {
        if (!hasToken()) return IssueSearchResult.NoToken
        return try {
            val response = api.searchIssues(query, perPage, page)
            val body = response.body
            if (!response.isSuccessful || body == null) {
                return IssueSearchResult.HttpError(
                    code = response.code,
                    rateLimited = response.code == 403 && response.rateLimit?.remaining == 0,
                )
            }
            RateLimitState.update(response.rateLimit)
            IssueSearchResult.Success(
                IssueSearchPage(
                    issues = body.items.map { it.toIssue(languageFilter = language.queryValue) },
                    totalCount = body.totalCount,
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            IssueSearchResult.Failed(e.message)
        }
    }

    companion object {
        /** The app-wide instance. Needs no Context, so no init call is required. */
        val instance: IssueSearchRepository by lazy { IssueSearchRepository(GitHubIssueSearchApi()) }
    }
}
