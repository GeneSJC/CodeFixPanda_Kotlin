package com.example.githubissuefinder.ui.discover

import com.example.githubissuefinder.data.TopReposEvent
import com.example.githubissuefinder.data.TopReposFailure
import com.example.githubissuefinder.data.TopReposState
import com.example.githubissuefinder.data.formatRelativeTime

/**
 * Turns TopReposService's facts into what TopReposScreen shows. The
 * service reports what happened (HTTP 401, no network, a rate limit);
 * the wording the user reads lives here, so it can change without
 * touching the data layer. Plain functions with no Android dependency,
 * so they are unit-tested directly.
 */
fun TopReposFailure.toMessage(): String = when (this) {
    is TopReposFailure.Http -> "GitHub returned an error (HTTP $code)"
    is TopReposFailure.Failed -> "Couldn't load issues: ${message ?: "unknown error"}"
}

fun TopReposState.toUiState(): TopReposUiState = TopReposUiState(
    repoGroups = repoGroups,
    isLoading = isLoading,
    errorMessage = loadFailure?.toMessage(),
    noTokenConfigured = noTokenConfigured,
    tokenRejected = tokenRejected,
    selectedLanguage = selectedLanguage,
    totalIssueCount = totalIssueCount,
    currentPage = currentPage,
    isLoadingMore = isLoadingMore,
    canLoadMore = canLoadMore,
    loadMoreError = loadMoreFailure?.toMessage(),
    lastUpdatedAtMs = lastUpdatedAtMs,
)

fun TopReposEvent.toMessage(): String = when (this) {
    TopReposEvent.RateLimited -> "GitHub's search rate limit was reached — try again shortly"
    is TopReposEvent.NewReposChecked ->
        if (newRepoCount == 0 && newIssueCount == 0) {
            "Nothing new since ${formatRelativeTime(previousUpdatedAtMs ?: checkedAtMs)}"
        } else {
            "$newRepoCount new repo${if (newRepoCount == 1) "" else "s"}, " +
                "$newIssueCount new issue${if (newIssueCount == 1) "" else "s"}"
        }
    is TopReposEvent.CheckFailedHttp -> "Couldn't check for new repos (HTTP $code)"
    is TopReposEvent.CheckFailed -> "Couldn't check for new repos: ${message ?: "unknown error"}"
}
