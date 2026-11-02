package io.github.genesjc.opensourcepanda.ui.discover

import io.github.genesjc.opensourcepanda.R
import io.github.genesjc.opensourcepanda.data.RepoIssuesState
import io.github.genesjc.opensourcepanda.model.SearchFailure
import io.github.genesjc.opensourcepanda.ui.StringResolver

/**
 * Turns RepoIssuesStateHolder's facts into what RepoIssuesScreen shows: the
 * wording lives here, so it can change without touching the data layer.
 * Plain functions that take the StringResolver, unit-tested directly with a fake.
 */
fun SearchFailure.toRefreshMessage(string: StringResolver): String = when (this) {
    is SearchFailure.Http -> string(R.string.error_http, code)
    is SearchFailure.Failed -> string(R.string.refresh_repo_failed, message ?: string(R.string.error_unknown))
}

fun RepoIssuesState.toUiState(string: StringResolver): RepoIssuesUiState = RepoIssuesUiState(
    issues = issues,
    isRefreshing = isRefreshing,
    hasRefreshed = hasRefreshed,
    lastRefreshedAtMs = lastRefreshedAtMs,
    refreshError = refreshFailure?.toRefreshMessage(string),
)
