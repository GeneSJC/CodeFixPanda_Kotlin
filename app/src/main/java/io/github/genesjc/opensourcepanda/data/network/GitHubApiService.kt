package io.github.genesjc.opensourcepanda.data.network

import io.github.genesjc.opensourcepanda.data.network.dto.IssueSearchResponseDto
import io.github.genesjc.opensourcepanda.data.network.dto.RateLimitResponseDto
import io.github.genesjc.opensourcepanda.data.network.dto.ReadmeDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Thin wrapper over GitHub's public REST API. searchIssues uses the Search API (page is 1-indexed)
 * and feeds IssueSearchRepository.rateLimit. getReadme uses the Contents API, a different quota
 * that is deliberately not tracked by the rate-limit chip.
 */
interface GitHubApiService {

    @GET("search/issues")
    suspend fun searchIssues(
        @Query("q") query: String,
        @Query("sort") sort: String = "updated",
        @Query("order") order: String = "desc",
        @Query("per_page") perPage: Int = 30,
        @Query("page") page: Int = 1,
    ): Response<IssueSearchResponseDto>

    /**
     * The current state of every rate-limit bucket. Free to call: it
     * doesn't count against any limit. Used to fill in the Search API
     * rate-limit chip when a list loads from the cache and no search
     * response has supplied the numbers yet.
     */
    @GET("rate_limit")
    suspend fun getRateLimit(): Response<RateLimitResponseDto>

    /**
     * GitHub's Contents API, not Search: see ReadmeDto for the rate-limit and base64 consequences.
     * 404s when a repo has no README at the root of its default branch, which RepoReadmeViewModel
     * treats as a real outcome, not an error.
     */
    @GET("repos/{owner}/{repo}/readme")
    suspend fun getReadme(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
    ): Response<ReadmeDto>
}
