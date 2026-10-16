package com.example.githubissuefinder.ui.discover

import com.example.githubissuefinder.data.ReadmeResult

data class RepoReadmeUiState(
    val isLoading: Boolean = true,
    val markdown: String? = null,
    /** True once a load has completed and found no README at all — a real, expected outcome, not an error. */
    val notFound: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * What the README screen shows for each way fetching can end. A pure
 * function, so each message and state can be unit-tested; the wording is
 * what the screen has always shown for these cases.
 */
fun ReadmeResult.toUiState(): RepoReadmeUiState = when (this) {
    is ReadmeResult.Found -> RepoReadmeUiState(isLoading = false, markdown = markdown)
    ReadmeResult.NotFound -> RepoReadmeUiState(isLoading = false, notFound = true)
    ReadmeResult.NoToken ->
        RepoReadmeUiState(isLoading = false, errorMessage = "A GitHub token is required to load a README.")
    is ReadmeResult.HttpError ->
        RepoReadmeUiState(isLoading = false, errorMessage = "GitHub returned an error (HTTP $code)")
    ReadmeResult.Unreadable ->
        RepoReadmeUiState(isLoading = false, errorMessage = "Couldn't read this README's content.")
    is ReadmeResult.Failed ->
        RepoReadmeUiState(isLoading = false, errorMessage = "Couldn't load the README: ${message ?: "unknown error"}")
}
