package com.example.githubissuefinder.network

import com.example.githubissuefinder.network.dto.IssueSearchResponseDto
import com.example.githubissuefinder.network.dto.RateLimitResponseDto
import com.example.githubissuefinder.network.dto.ReadmeDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Thin wrapper over GitHub's public REST API.
 *
 * searchIssues uses the Search API — wrapped in Response<> and feeds
 * IssueSearchRepository.rateLimit (see the ViewModels that call it).
 *
 * v0.42: searchRepositories (and RepoDto/RepoSearchResponseDto) removed
 * — the repo-first Discover flow no longer queries GitHub's repository
 * search at all. It now builds its repo list entirely by grouping
 * searchIssues results by their parent repo (see RepoIssueGroup.kt and
 * TopReposService's class comment for the full reasoning). Nothing
 * else in this codebase called searchRepositories — confirmed before
 * removing it, not assumed.
 *
 * v0.43: added the page parameter, for Top Repos' "Load More" —
 * GitHub's Search API is 1-indexed (page 1 is the first page, not 0).
 * Everything else about this call is unchanged; existing callers that
 * don't pass page keep working exactly as before via the default.
 *
 * Note: an earlier version of this file also had a getIssueComments()
 * method, targeting GitHub's general REST API (a separate rate-limit
 * bucket) for the Detail-screen Comments tab. That whole feature was
 * shelved and never applied to this project, so that method — and its
 * CommentDto return type, which doesn't exist here — are not included.
 * If that work is picked back up later, re-add it then.
 *
 * getReadme (added for the repo README viewer) is this codebase's
 * first ACTUALLY-applied use of that general REST API bucket — the
 * Contents API, not Search. Its own doc comment, and ReadmeDto's,
 * cover why its rate-limit quota is deliberately not tracked by
 * IssueSearchRepository.rateLimit the way searchIssues' is.
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
     * GitHub's Contents API, not Search — see ReadmeDto's own doc
     * comment for the rate-limit and base64-encoding consequences of
     * that. 404s when a repo genuinely has no README at the root of
     * its default branch (RepoReadmeViewModel treats that as a real,
     * expected outcome, not an error).
     */
    @GET("repos/{owner}/{repo}/readme")
    suspend fun getReadme(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
    ): Response<ReadmeDto>
}
