package com.example.githubissuefinder.model

/**
 * A repository as the "Discover" repo-first flow displays it (see
 * ui/discover/ — CM 1 of the option-C redesign).
 *
 * score is the composite "interesting repo" ranking value (see
 * data/RepoScoring.kt) — kept on the model so the UI can show it
 * directly, matching this project's "every number traces to something
 * real and visible" convention rather than hiding the ranking logic.
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
)
