package com.example.githubissuefinder.ui.discover

data class RepoReadmeUiState(
    val isLoading: Boolean = true,
    val markdown: String? = null,
    /** True once a load has completed and found no README at all — a real, expected outcome, not an error. */
    val notFound: Boolean = false,
    val errorMessage: String? = null,
)
