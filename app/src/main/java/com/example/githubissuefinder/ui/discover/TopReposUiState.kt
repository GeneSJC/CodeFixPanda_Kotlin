package com.example.githubissuefinder.ui.discover

import com.example.githubissuefinder.model.LanguageFilter
import com.example.githubissuefinder.model.RepoIssueGroup

data class TopReposUiState(
    /** v0.42: repos: List<Repo> replaced by repoGroups: List<RepoIssueGroup> — see TopReposViewModel's doc comment. */
    val repoGroups: List<RepoIssueGroup> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /**
     * True when no GitHub token is configured. This screen has NO
     * sample-data fallback yet, unlike Feed (v0.10) — deliberately out
     * of scope for CM 1, since the "Discover" flow is still being
     * proven out. A token is required to see anything here for now.
     */
    val noTokenConfigured: Boolean = false,
    /** Same LanguageFilter enum Feed uses (v0.8) — shared, not a second set of options. */
    val selectedLanguage: LanguageFilter = LanguageFilter.KOTLIN,
    /**
     * Total matching ISSUES GitHub reports for the current query
     * (v0.42) — not a repo count. Mirrors Feed's totalResultCount;
     * shown so "here are the top N repos with matches" has an honest
     * "out of M total matching issues" frame, same transparency
     * instinct the old ranking dialog had for its scoring formula.
     */
    val totalIssueCount: Int = 0,
    // cooldownSecondsRemaining REMOVED in v0.42 — the old 60-second
    // cooldown existed because one load cost ~21 Search API calls (1
    // repo search + 20 per-repo annotation checks), so a second load
    // within 60s already risked the 30/min cap. This screen's ENTIRE
    // load is now a single issue-search call — a genuine architectural
    // side benefit of the pipeline swap, not something separately
    // decided. See TopReposViewModel's doc comment.

    /**
     * v0.43 (pagination): which page of the current query has been
     * fetched so far — 1 immediately after the initial load(), Load
     * More increments it. Reset to 1 on every fresh load() (language
     * or mode switch, or retry()).
     */
    val currentPage: Int = 1,
    /**
     * True while a Load More fetch is in flight — deliberately
     * separate from isLoading, which gates the FULL-SCREEN spinner for
     * the very first load. Load More shows its own small footer
     * indicator instead, so the already-visible list never disappears
     * behind a spinner just because more is being fetched underneath.
     */
    val isLoadingMore: Boolean = false,
    /**
     * False once there's genuinely nothing further to fetch — either
     * GitHub returned a page with fewer than a full page's worth of
     * results (the actual end of this query's results), or the next
     * page would exceed the Search API's hard 1,000-result cap. See
     * TopReposViewModel.canRequestNextPage() for the exact check.
     */
    val canLoadMore: Boolean = true,
    /**
     * Set when a Load More fetch fails — deliberately NOT reusing
     * errorMessage (which is scoped to "the whole list failed to
     * load"). A failed Load More leaves the existing list exactly as
     * it was; only the footer reflects the failure, with its own retry
     * affordance (TopReposViewModel.retryLoadMore()).
     */
    val loadMoreError: String? = null,
)
