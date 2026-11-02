package io.github.genesjc.opensourcepanda.data

import io.github.genesjc.opensourcepanda.model.Issue

/** One page of search results, already converted to the app's own Issue type. */
data class IssueSearchPage(val issues: List<Issue>, val totalCount: Int)

/**
 * Every way a search can end, so callers react to an outcome instead of
 * poking at an HTTP response themselves.
 */
sealed interface IssueSearchResult {
    data class Success(val page: IssueSearchPage) : IssueSearchResult

    /**
     * GitHub answered with an error. [rateLimited] is true for a 403 whose
     * rate-limit headers say nothing is left — GitHub's own signal for
     * "slow down," distinct from a 401, which means the token itself was
     * rejected (check [code]).
     */
    data class HttpError(val code: Int, val rateLimited: Boolean) : IssueSearchResult

    /** The request itself failed (no network, a timeout, ...). */
    data class Failed(val message: String?) : IssueSearchResult
}
