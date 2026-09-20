package com.example.githubissuefinder.ui.discover

import com.example.githubissuefinder.model.LanguageFilter
import com.example.githubissuefinder.model.Repo

data class TopReposUiState(
    val repos: List<Repo> = emptyList(),
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
     * Seconds remaining before another language switch is allowed
     * (v0.35). 0 means switching is currently allowed. Exists because
     * each load fires 21 Search API calls (1 repo search + 20
     * per-repo good-first-issue checks, v0.34) — a second load within
     * 60 seconds of the first already exceeds the 30/min Search API
     * cap, not just a third or later one.
     */
    val cooldownSecondsRemaining: Int = 0,
)
