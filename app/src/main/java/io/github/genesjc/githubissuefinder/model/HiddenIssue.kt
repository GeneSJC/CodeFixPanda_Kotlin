package io.github.genesjc.githubissuefinder.model

/**
 * A minimal snapshot of a hidden issue, not the full Issue: just enough to list and re-open it, so
 * the Hidden list still works after the issue's cache entry is gone (Clear Cache does not touch
 * hidden issues). Keyed by GitHub's issue id, which is globally unique.
 */
data class HiddenIssue(
    val id: Long,
    val title: String,
    val repoFullName: String,
    val htmlUrl: String,
    val hiddenAtMs: Long,
    /** The tag of the issue when it was hidden (see Issue.tag). */
    val tag: String? = null,
)
