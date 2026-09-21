package com.example.githubissuefinder.network

import com.example.githubissuefinder.network.dto.IssueSearchResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Thin wrapper over GitHub's public REST API.
 *
 * searchIssues uses the Search API — wrapped in Response<> and feeds
 * RateLimitState (see the ViewModels that call it).
 *
 * v0.42: searchRepositories (and RepoDto/RepoSearchResponseDto) removed
 * — the repo-first Discover flow no longer queries GitHub's repository
 * search at all. It now builds its repo list entirely by grouping
 * searchIssues results by their parent repo (see RepoIssueGroup.kt and
 * TopReposViewModel's doc comment for the full reasoning). Nothing
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
}
