package com.example.githubissuefinder.ui.discover

import com.example.githubissuefinder.data.RepoIssuesState
import com.example.githubissuefinder.data.SearchFailure

/**
 * Turns RepoIssuesStateHolder's facts into what RepoIssuesScreen shows: the
 * wording lives here, so it can change without touching the data layer.
 * Plain functions, unit-tested directly.
 */
fun SearchFailure.toRefreshMessage(): String = when (this) {
    is SearchFailure.Http -> "GitHub returned an error (HTTP $code)"
    is SearchFailure.Failed -> "Couldn't refresh this repo: ${message ?: "unknown error"}"
}

fun RepoIssuesState.toUiState(): RepoIssuesUiState = RepoIssuesUiState(
    issues = issues,
    isRefreshing = isRefreshing,
    hasRefreshed = hasRefreshed,
    lastRefreshedAtMs = lastRefreshedAtMs,
    refreshError = refreshFailure?.toRefreshMessage(),
)
