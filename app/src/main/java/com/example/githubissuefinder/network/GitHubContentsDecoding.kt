package com.example.githubissuefinder.network

import android.util.Base64

/**
 * Shared base64 decoding for GitHub's Contents API responses. Extracted
 * so RepoReadmeViewModel's full-README fetch and
 * RepoReadmeSnippetStore's short on-card preview don't each carry their
 * own copy of this — both call the same Contents API endpoint
 * (GitHubApiService.getReadme) and need the exact same decoding step.
 *
 * GitHub's Contents API returns content as base64 WITH embedded
 * newlines every 60 characters (its own line-wrapping convention, not
 * an encoding error) — android.util.Base64 rejects those newlines
 * outright unless stripped first, so this isn't optional cleanup.
 * encoding is checked explicitly rather than assumed: GitHub documents
 * "base64" as the only value this endpoint returns today, but failing
 * loudly on anything else is cheap insurance against silently showing
 * garbled text if that ever changes.
 */
fun decodeGitHubContentsBase64(content: String?, encoding: String?): String? {
    if (content == null || encoding != "base64") return null
    return try {
        String(Base64.decode(content.replace("\n", ""), Base64.DEFAULT), Charsets.UTF_8)
    } catch (e: IllegalArgumentException) {
        null
    }
}
