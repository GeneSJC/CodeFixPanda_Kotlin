package com.example.githubissuefinder.network

import com.example.githubissuefinder.network.dto.IssueSearchResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Thin wrapper over GitHub's public REST Search API. The "q" param uses
 * GitHub's own search qualifier syntax (label:"..." language:... state:...),
 * built by FeedViewModel — this interface just knows how to send it.
 *
 * Returns Response<IssueSearchResponseDto> rather than the bare DTO, so
 * callers can read response headers (X-RateLimit-Remaining/-Limit) in
 * addition to the parsed body — this is what powers the rate-limit badge.
 */
interface GitHubApiService {

    @GET("search/issues")
    suspend fun searchIssues(
        @Query("q") query: String,
        @Query("sort") sort: String = "updated",
        @Query("order") order: String = "desc",
        @Query("per_page") perPage: Int = 30,
    ): Response<IssueSearchResponseDto>
}
