package com.example.githubissuefinder.network.dto

import com.squareup.moshi.Json

/**
 * Raw shape of GET /repos/{owner}/{repo}/readme, GitHub's Contents API. It is billed against the
 * general REST quota (5,000 requests/hour authenticated), not the Search API's 30/minute, so it is
 * deliberately not counted by IssueSearchRepository.rateLimit; mixing them would misrepresent both.
 * content is base64 with embedded newlines (see GitHubContentsDecoding.kt). encoding is modeled
 * explicitly so an unexpected value fails loudly.
 */
data class ReadmeDto(
    val name: String,
    val content: String?,
    val encoding: String?,
    @Json(name = "html_url") val htmlUrl: String?,
)
