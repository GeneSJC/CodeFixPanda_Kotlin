package io.github.genesjc.codefixpanda.state

/** One-shot things the screen should tell the user, reported as facts. The ViewModel turns them into the Snackbar text. */
sealed interface TopReposEvent {
    /** A Load More hit GitHub's search rate limit. */
    data object RateLimited : TopReposEvent

    /** Check for New Repos finished; [previousUpdatedAtMs] is the list's stamp before this check (null if it had none). */
    data class NewReposChecked(
        val newRepoCount: Int,
        val newIssueCount: Int,
        val previousUpdatedAtMs: Long?,
        val checkedAtMs: Long,
    ) : TopReposEvent

    /** Check for New Repos got an error status from GitHub. */
    data class CheckFailedHttp(val code: Int) : TopReposEvent

    /** Check for New Repos failed before GitHub answered. */
    data class CheckFailed(val message: String?) : TopReposEvent
}
