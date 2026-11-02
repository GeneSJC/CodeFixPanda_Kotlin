package io.github.genesjc.opensourcepanda.data

import io.github.genesjc.opensourcepanda.data.network.GitHubApiService
import io.github.genesjc.opensourcepanda.data.network.dto.ReadmeDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** What came back from the README endpoint: the HTTP status, and the body if there was one. */
data class ReadmeApiResponse(val code: Int, val isSuccessful: Boolean, val body: ReadmeDto?)

/**
 * The one call this repository makes, split out so the repository's own
 * rules can be unit-tested against a plain fake with no network.
 */
fun interface ReadmeApi {
    suspend fun fetchReadme(repoOwner: String, repoName: String): ReadmeApiResponse
}

/**
 * GitHub's Contents API (GET /repos/{owner}/{repo}/readme), run off the main thread. A different
 * API and quota from search, so README fetches are deliberately not counted by
 * IssueSearchRepository.rateLimit.
 */
class GitHubReadmeApi(private val service: GitHubApiService) : ReadmeApi {
    override suspend fun fetchReadme(repoOwner: String, repoName: String): ReadmeApiResponse =
        withContext(Dispatchers.IO) {
            val response = service.getReadme(repoOwner, repoName)
            ReadmeApiResponse(response.code(), response.isSuccessful, response.body())
        }
}
