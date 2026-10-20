package io.github.genesjc.githubissuefinder.network

import android.util.Base64

/**
 * Shared base64 decoding for GitHub's Contents API responses. GitHub wraps the base64 with newlines
 * every 60 characters, which android.util.Base64 rejects unless they are stripped, so the stripping
 * is required. The encoding is checked explicitly: failing loudly on anything but "base64" is cheap
 * insurance against silently showing garbled text.
 */
fun decodeGitHubContentsBase64(content: String?, encoding: String?): String? {
    if (content == null || encoding != "base64") return null
    return try {
        String(Base64.decode(content.replace("\n", ""), Base64.DEFAULT), Charsets.UTF_8)
    } catch (e: IllegalArgumentException) {
        null
    }
}
