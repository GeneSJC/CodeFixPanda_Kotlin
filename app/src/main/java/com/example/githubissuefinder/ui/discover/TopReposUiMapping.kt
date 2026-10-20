package com.example.githubissuefinder.ui.discover

import com.example.githubissuefinder.data.TopReposEvent
import com.example.githubissuefinder.data.SearchFailure
import com.example.githubissuefinder.data.TopReposState
import com.example.githubissuefinder.R
import com.example.githubissuefinder.ui.relativeTime
import com.example.githubissuefinder.ui.StringResolver

/**
 * Turns TopReposStateHolder's facts into what TopReposScreen shows. The
 * state holder reports what happened (HTTP 401, no network, a rate limit);
 * the wording the user reads lives here, so it can change without
 * touching the data layer. Plain functions that take the [StringResolver],
 * so they are unit-tested directly with a fake.
 */
fun SearchFailure.toMessage(string: StringResolver): String = when (this) {
    is SearchFailure.Http -> string(R.string.error_http, code)
    is SearchFailure.Failed -> string(R.string.load_issues_failed, message ?: string(R.string.error_unknown))
}

fun TopReposState.toUiState(string: StringResolver): TopReposUiState = TopReposUiState(
    repoGroups = repoGroups,
    isLoading = isLoading,
    errorMessage = loadFailure?.toMessage(string),
    tokenRejected = tokenRejected,
    selectedLanguage = selectedLanguage,
    totalIssueCount = totalIssueCount,
    currentPage = currentPage,
    isLoadingMore = isLoadingMore,
    canLoadMore = canLoadMore,
    loadMoreError = loadMoreFailure?.toMessage(string),
    lastUpdatedAtMs = lastUpdatedAtMs,
)

fun TopReposEvent.toMessage(string: StringResolver): String = when (this) {
    TopReposEvent.RateLimited -> string(R.string.search_rate_limited)
    is TopReposEvent.NewReposChecked ->
        if (newRepoCount == 0 && newIssueCount == 0) {
            string(R.string.nothing_new_since, string.relativeTime(previousUpdatedAtMs ?: checkedAtMs))
        } else {
            string(
                R.string.new_repos_and_issues,
                string.plural(R.plurals.new_repos, newRepoCount, newRepoCount),
                string.plural(R.plurals.new_issues, newIssueCount, newIssueCount),
            )
        }
    is TopReposEvent.CheckFailedHttp -> string(R.string.check_new_repos_failed_http, code)
    is TopReposEvent.CheckFailed -> string(R.string.check_new_repos_failed, message ?: string(R.string.error_unknown))
}
