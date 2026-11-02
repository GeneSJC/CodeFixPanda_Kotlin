package io.github.genesjc.opensourcepanda.data

import io.github.genesjc.opensourcepanda.network.GitHubApiService
import io.github.genesjc.opensourcepanda.network.dto.IssueSearchResponseDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** What came back from the Search API: the HTTP status, the body if any, and the rate-limit headers it carried. */
data class IssueSearchApiResponse(
    val code: Int,
    val isSuccessful: Boolean,
    val body: IssueSearchResponseDto?,
    val rateLimit: RateLimitInfo?,
    /** The tag to put on the issues in [body] (see Issue.tag): null for real GitHub data. */
    val tag: String? = null,
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

/** The real calls, run off the main thread through the Retrofit service. */
class GitHubIssueSearchApi(private val service: GitHubApiService) : IssueSearchApi {

    override suspend fun searchIssues(query: String, perPage: Int, page: Int): IssueSearchApiResponse =
        withContext(Dispatchers.IO) {
            val response = service.searchIssues(query = query, perPage = perPage, page = page)
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
                val response = service.getRateLimit()
                val body = response.body()
                if (response.isSuccessful && body != null) body.toSearchRateLimitInfo() else null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        }
}
