package com.example.githubissuefinder.model

/**
 * v0.51 ("Hide Issue", Part C of the Cache-Preserving Refresh sequence):
 * a minimal, standalone snapshot of a hidden issue — NOT the full Issue
 * model. Kept deliberately small (just enough to list and re-open the
 * issue later) so the Hidden list can still show and open an issue even
 * after its RepoIssueGroupRepository entry is gone entirely (e.g. after
 * Clear Cache, which does NOT touch this store — see HiddenIssuesRepository).
 *
 * Keyed by GitHub's own issue id wherever it's stored — globally
 * unique, so no repo-scoping is needed on the key itself.
 */
data class HiddenIssue(
    val id: Long,
    val title: String,
    val repoFullName: String,
    val htmlUrl: String,
    val hiddenAtMs: Long,
)
