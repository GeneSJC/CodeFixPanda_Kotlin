package io.github.genesjc.codefixpanda.ui.toprepos

import io.github.genesjc.codefixpanda.model.LanguageFilter
import io.github.genesjc.codefixpanda.model.RepoIssueGroup

data class TopReposUiState(
    val repoGroups: List<RepoIssueGroup> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /**
     * True when a token is configured but GitHub rejected it (HTTP 401, distinct from a 403 rate limit).
     * Its own field, not folded into errorMessage, so the rate-limit chip's tap can tell a token that
     * does not work from a failed load.
     */
    val tokenRejected: Boolean = false,
    /** The language the list is for; the same LanguageFilter options the rest of the app uses. */
    val selectedLanguage: LanguageFilter = LanguageFilter.KOTLIN,
    /**
     * Total matching ISSUES GitHub reports for the current query, not a repo count. Shown so
     * "the top N repos with matches" has an honest "out of M matching issues" frame.
     */
    val totalIssueCount: Int = 0,
    /**
     * Which page of the current query has been fetched so far: 1 after the initial load, and Load
     * More increments it. Reset to 1 on every fresh load (language or mode switch, or retry).
     */
    val currentPage: Int = 1,
    /**
     * True while a Load More fetch is in flight. Separate from isLoading, which gates the full-screen
     * spinner of the first load: Load More shows a small footer indicator, so the visible list never
     * disappears behind a spinner while more is fetched.
     */
    val isLoadingMore: Boolean = false,
    /**
     * False once there is nothing further to fetch: GitHub returned less than a full page (the end of
     * this query's results), or the next page would pass the Search API's 1,000-result cap. See
     * canRequestNextPage() (domain/TopReposPaging.kt).
     */
    val canLoadMore: Boolean = true,
    /**
     * Set when a Load More fetch fails. Not errorMessage, which means "the whole list failed to
     * load": a failed Load More leaves the list as it was, and only the footer reflects it, with its
     * own retry (TopReposViewModel.retryLoadMore()).
     */
    val loadMoreError: String? = null,
    /**
     * When the shown repoGroups were fetched, live or from the cache. Null before the first load
     * resolves. Drives "Updated X ago", so cached data never pretends to be fresher than it is.
     */
    val lastUpdatedAtMs: Long? = null,
) {
    /**
     * True while the list can't load because GitHub rejected the token in use. When the token changes in
     * this state, TopReposStateHolder reloads.
     */
    val needsTokenFix: Boolean
        get() = tokenRejected
}
