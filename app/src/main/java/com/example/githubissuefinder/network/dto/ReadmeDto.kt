package com.example.githubissuefinder.network.dto

import com.squareup.moshi.Json

/**
 * Raw shape returned by GET /repos/{owner}/{repo}/readme — GitHub's
 * Contents API, not the Search API everything else in this app's
 * network layer uses. Two consequences worth knowing:
 *
 * 1. It's billed against the general REST API's much larger quota
 *    (5,000 requests/hour authenticated) rather than the Search API's
 *    30/minute — IssueSearchRepository.rateLimit is deliberately NOT updated from this
 *    call (see RepoReadmeViewModel), since mixing the two quotas into
 *    one tracked number would misrepresent both.
 * 2. content is base64-encoded with embedded newlines, per GitHub's
 *    own Contents API convention — see RepoReadmeViewModel.decode()
 *    for why the newlines have to be stripped before real base64
 *    decoding, not just an escaping quirk to ignore.
 *
 * encoding is almost always "base64" for this endpoint as GitHub
 * documents it, but is still modeled explicitly (not assumed) so a
 * future unexpected value fails loudly in RepoReadmeViewModel rather
 * than silently producing garbled text.
 */
data class ReadmeDto(
    val name: String,
    val content: String?,
    val encoding: String?,
    @Json(name = "html_url") val htmlUrl: String?,
)
