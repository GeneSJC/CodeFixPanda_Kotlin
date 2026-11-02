package io.github.genesjc.opensourcepanda.state

import io.github.genesjc.opensourcepanda.model.LanguageFilter
import io.github.genesjc.opensourcepanda.model.RepoIssueGroup
import io.github.genesjc.opensourcepanda.model.SearchFailure

/**
 * The Top Repos data state: everything the screen's list is built from,
 * and nothing about how it is shown. TopReposStateHolder is the one place it
 * lives and changes; TopReposViewModel maps it to TopReposUiState (which
 * adds the message text).
 */
data class TopReposState(
    /** The repo groups shown, ranked by match count; see RepoIssueGroup. */
    val repoGroups: List<RepoIssueGroup> = emptyList(),
    val selectedLanguage: LanguageFilter = LanguageFilter.KOTLIN,
    /** Total matching ISSUES GitHub reports for the current query (not a repo count). */
    val totalIssueCount: Int = 0,
    /** Which page of the current query has been fetched so far; reset on every fresh load. */
    val currentPage: Int = 1,
    /** False once there is nothing further to fetch (a short page, or GitHub's 1,000-result cap). */
    val canLoadMore: Boolean = true,
    /** When the shown repoGroups were fetched: a live call, or the cache entry's own stamp. Null before the first load resolves. */
    val lastUpdatedAtMs: Long? = null,
    /** True while the initial load (the full-screen one) is in flight. */
    val isLoading: Boolean = false,
    /** True while a Load More is in flight, kept separate from [isLoading] so the visible list never disappears behind a spinner. */
    val isLoadingMore: Boolean = false,
    /** True when a token is configured but GitHub rejected it (HTTP 401). */
    val tokenRejected: Boolean = false,
    /** Set when the whole list failed to load. */
    val loadFailure: SearchFailure? = null,
    /** Set when a Load More failed; the list itself is left exactly as it was. */
    val loadMoreFailure: SearchFailure? = null,
) {
    /** True while the list cannot load because GitHub rejected the token in use. */
    val needsTokenFix: Boolean
        get() = tokenRejected
}
