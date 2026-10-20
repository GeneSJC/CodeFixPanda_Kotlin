package com.example.githubissuefinder.model

import com.example.githubissuefinder.data.humanizeRepoName

/**
 * A single GitHub issue as the app displays it, mapped from the API response (see IssueMappers.kt).
 * repoDisplayTitle is a humanized repo name, a computed getter like repoFullName; see
 * RepoNameFormatting.kt for why it is an approximation.
 */
data class Issue(
    val id: Long,
    val title: String,
    val bodyText: String,
    val repoOwner: String,
    val repoName: String,
    val labels: List<IssueLabel>,
    val language: String?,
    val commentCount: Int,
    /** When GitHub last updated the issue (epoch millis); null if the timestamp could not be read. */
    val updatedAtMs: Long? = null,
    /** Public GitHub URL for this issue — used by the "Open in GitHub" action. */
    val htmlUrl: String,
) {
    val repoFullName: String get() = "$repoOwner/$repoName"
    val repoDisplayTitle: String get() = humanizeRepoName(repoName)
}

data class IssueLabel(
    val name: String,
    /** Hex color as GitHub returns it, without the leading '#'. */
    val colorHex: String,
)
