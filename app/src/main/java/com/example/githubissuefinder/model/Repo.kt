package com.example.githubissuefinder.model

/**
 * A repository as the "Discover" repo-first flow displays it (see
 * ui/discover/ — CM 1 of the option-C redesign).
 *
 * score is the composite "interesting repo" ranking value (see
 * data/RepoScoring.kt) — kept on the model so the UI can show it
 * directly, matching this project's "every number traces to something
 * real and visible" convention rather than hiding the ranking logic.
 *
 * goodFirstIssueCount (v0.34) is the actual number of currently-open
 * issues labeled "good first issue" in this repo — null while that
 * check hasn't completed yet (a separate call per repo, made after the
 * ranked list itself loads), never a placeholder for "zero." Added to
 * directly address the known limitation flagged since CM 1: a repo can
 * rank highly on general health while having zero matching issues.
 * Ranking itself does NOT yet factor this in — that's a planned,
 * separate follow-up CM.
 *
 * starsPerDay (v0.37) is a "how fast is this actually catching on"
 * velocity signal — stars divided by days since the repo was created —
 * aimed at making an already-popular repo feel like it's happening
 * RIGHT NOW rather than just being a static, settled number. Null for
 * repos younger than RepoScoring's minimum-age threshold, where the
 * metric would be noise (a brand-new repo with 50 stars on day one
 * looks "explosive" but isn't a real signal yet) — not a placeholder
 * for zero.
 */
data class Repo(
    val id: Long,
    val fullName: String,
    val ownerLogin: String,
    val name: String,
    val description: String,
    val stars: Int,
    val forks: Int,
    val openIssuesCount: Int,
    val language: String?,
    val updatedAtLabel: String,
    val htmlUrl: String,
    val score: Float,
    val goodFirstIssueCount: Int? = null,
    val starsPerDay: Float? = null,
)
