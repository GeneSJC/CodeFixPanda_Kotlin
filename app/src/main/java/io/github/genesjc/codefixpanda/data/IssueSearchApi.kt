package io.github.genesjc.codefixpanda.data

import io.github.genesjc.codefixpanda.data.network.GitHubApiService
import io.github.genesjc.codefixpanda.data.network.dto.IssueSearchResponseDto
import io.github.genesjc.codefixpanda.model.DataOrigin
import io.github.genesjc.codefixpanda.model.RateLimitInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** What came back from the Search API: the HTTP status, the body if any, and the rate-limit headers it carried. */
data class IssueSearchApiResponse(
    val code: Int,
    val isSuccessful: Boolean,
    val body: IssueSearchResponseDto?,
    val rateLimit: RateLimitInfo?,
    /** The origin to put on the issues in [body] (see Issue.origin). */
    val origin: DataOrigin = DataOrigin.GITHUB,
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
                // Rethrown so cancelling the caller still cancels this call; only real failures become "unknown".
                throw e
            } catch (e: Exception) {
                // The numbers are an extra: a failed lookup means "not known yet", and the caller keeps what it had.
                null
            }
        }
}
