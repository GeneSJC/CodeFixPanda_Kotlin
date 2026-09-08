package com.example.githubissuefinder.model

/**
 * Represents a single GitHub issue as our app displays it.
 *
 * For now this is populated with hardcoded sample data (see SampleIssues.kt).
 * Once Retrofit is wired in, a mapper will convert the raw GitHub API
 * response into this same shape — the UI layer never needs to change.
 */
data class Issue(
    val id: Long,
    val title: String,
    val repoOwner: String,
    val repoName: String,
    val labels: List<IssueLabel>,
    val language: String?,
    val commentCount: Int,
    val updatedAtLabel: String,
    val isBookmarked: Boolean = false,
) {
    val repoFullName: String get() = "$repoOwner/$repoName"
}

data class IssueLabel(
    val name: String,
    /** Hex color as GitHub returns it, without the leading '#'. */
    val colorHex: String,
)
