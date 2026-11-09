package io.github.genesjc.codefixpanda.data

/**
 * Every way fetching a repo's README can end, so callers react to an
 * outcome instead of poking at an HTTP response themselves.
 */
sealed interface ReadmeResult {
    /** The README's decoded markdown text. */
    data class Found(val markdown: String) : ReadmeResult

    /** The repo has no README at the root of its default branch (a real, expected 404, not an error). */
    data object NotFound : ReadmeResult

    /** No GitHub token is in use, so nothing was requested. */

    /** GitHub answered, but with an error (anything other than success or the 404 above). */
    data class HttpError(val code: Int) : ReadmeResult

    /** GitHub answered with a README the app couldn't decode. */
    data object Unreadable : ReadmeResult

    /** The request itself failed (no network, a timeout, ...): worth trying again later. */
    data class Failed(val message: String?) : ReadmeResult
}
