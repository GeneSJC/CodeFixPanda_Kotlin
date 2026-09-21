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
)
