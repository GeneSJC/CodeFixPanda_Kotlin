package com.example.githubissuefinder.network.dto

import com.squareup.moshi.Json

/**
 * Raw shapes returned by GET /search/repositories.
 *
 * UserDto is defined directly here, NOT reused from a "CommentDto.kt" —
 * that file belongs to the original v0.24 (Detail screen Comments tab),
 * which was explicitly shelved and never applied to this project. An
 * earlier version of this file incorrectly assumed UserDto already
 * existed from that CM; it doesn't, so it's defined here instead. If the
 * Comments-tab work is ever picked back up later, its own DTO should
 * reuse THIS UserDto (same package), not redefine a second one.
 *
 * This is the REST Search API — the same rate-limit bucket issue search
 * uses (confirmed in the API surface reference), not a separate one.
 */
data class RepoSearchResponseDto(
    @Json(name = "total_count") val totalCount: Int = 0,
    @Json(name = "items") val items: List<RepoDto> = emptyList(),
)

data class RepoDto(
    val id: Long,
    val name: String,
    @Json(name = "full_name") val fullName: String,
    val owner: UserDto,
    val description: String?,
    @Json(name = "stargazers_count") val stargazersCount: Int,
    @Json(name = "forks_count") val forksCount: Int,
    @Json(name = "open_issues_count") val openIssuesCount: Int,
    val language: String?,
    @Json(name = "updated_at") val updatedAt: String,
    @Json(name = "html_url") val htmlUrl: String,
)

data class UserDto(
    val login: String,
    @Json(name = "avatar_url") val avatarUrl: String,
)
