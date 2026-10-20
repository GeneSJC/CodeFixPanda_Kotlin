package io.github.genesjc.githubissuefinder.network.dto

import com.squareup.moshi.Json

/**
 * Raw shapes returned by GET /search/issues. Field names use @Json(name=)
 * to map GitHub's snake_case JSON onto Kotlin camelCase properties.
 * Parsed via Moshi's reflection-based adapter (KotlinJsonAdapterFactory),
 * not codegen — no KSP/annotation-processor setup required.
 */
data class IssueSearchResponseDto(
    @Json(name = "total_count") val totalCount: Int = 0,
    @Json(name = "items") val items: List<IssueDto> = emptyList(),
)

data class IssueDto(
    val id: Long,
    val number: Int,
    val title: String,
    val body: String?,
    val state: String,
    @Json(name = "html_url") val htmlUrl: String,
    @Json(name = "repository_url") val repositoryUrl: String,
    val comments: Int,
    @Json(name = "updated_at") val updatedAt: String,
    val labels: List<LabelDto> = emptyList(),
)

data class LabelDto(
    val name: String,
    val color: String,
)
