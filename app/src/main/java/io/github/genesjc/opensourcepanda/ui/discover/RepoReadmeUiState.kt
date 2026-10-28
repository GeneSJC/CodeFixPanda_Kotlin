package io.github.genesjc.opensourcepanda.ui.discover

import io.github.genesjc.opensourcepanda.R
import io.github.genesjc.opensourcepanda.data.ReadmeResult
import io.github.genesjc.opensourcepanda.ui.StringResolver

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
 * what the screen has always shown for these cases, now from string resources.
 */
fun ReadmeResult.toUiState(string: StringResolver): RepoReadmeUiState = when (this) {
    is ReadmeResult.Found -> RepoReadmeUiState(isLoading = false, markdown = markdown)
    ReadmeResult.NotFound -> RepoReadmeUiState(isLoading = false, notFound = true)
    is ReadmeResult.HttpError ->
        RepoReadmeUiState(isLoading = false, errorMessage = string(R.string.error_http, code))
    ReadmeResult.Unreadable ->
        RepoReadmeUiState(isLoading = false, errorMessage = string(R.string.readme_unreadable))
    is ReadmeResult.Failed ->
        RepoReadmeUiState(isLoading = false, errorMessage = string(R.string.readme_load_failed, message ?: string(R.string.error_unknown)))
}
