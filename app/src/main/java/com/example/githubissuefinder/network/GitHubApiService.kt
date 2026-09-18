package com.example.githubissuefinder.network

import com.example.githubissuefinder.network.dto.IssueSearchResponseDto
import com.example.githubissuefinder.network.dto.RepoSearchResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Thin wrapper over GitHub's public REST API.
 *
 * searchIssues and searchRepositories both use the Search API — the SAME
 * rate-limit bucket, confirmed in the API surface reference — so both are
 * wrapped in Response<> and both feed RateLimitState (see their
 * respective ViewModels).
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
    ): Response<IssueSearchResponseDto>

    @GET("search/repositories")
    suspend fun searchRepositories(
        @Query("q") query: String,
        @Query("sort") sort: String = "stars",
        @Query("order") order: String = "desc",
        @Query("per_page") perPage: Int = 50,
    ): Response<RepoSearchResponseDto>
}
