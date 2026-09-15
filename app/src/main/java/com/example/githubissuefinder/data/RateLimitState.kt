package com.example.githubissuefinder.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * remaining/limit as reported by GitHub's Search API on its own responses
 * (X-RateLimit-Remaining / X-RateLimit-Limit headers). Worth noting: the
 * Search API has its own, separate, stricter rate limit from GitHub's
 * general REST API — these numbers reflect search specifically, not the
 * broader 5000/hour core API figure some might expect.
 */
data class RateLimitInfo(val remaining: Int, val limit: Int)

/**
 * Plain in-memory singleton — same temporary, not-Hilt-managed pattern as
 * IssueCache/CurrentFeedState/GitHubApiClient. Null means "no live search
 * has completed yet," or "currently in sample-data mode" (no token
 * configured), where a rate limit isn't applicable at all.
 *
 * Deliberately independent of CurrentFeedState/FeedUiState: this reflects
 * account/session state (the credential's shared quota), not anything
 * about the current search's results, so it doesn't belong bundled with
 * either of those.
 */
object RateLimitState {

    private val _info = MutableStateFlow<RateLimitInfo?>(null)
    val info: StateFlow<RateLimitInfo?> = _info.asStateFlow()

    fun update(newInfo: RateLimitInfo?) {
        _info.value = newInfo
    }
}
