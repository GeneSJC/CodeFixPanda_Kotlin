package com.example.githubissuefinder.ui.discover

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
)
