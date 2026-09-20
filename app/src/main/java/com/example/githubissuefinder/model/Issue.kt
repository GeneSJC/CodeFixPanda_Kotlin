package com.example.githubissuefinder.model

import com.example.githubissuefinder.data.humanizeRepoName

/**
 * Represents a single GitHub issue as our app displays it.
 *
 * For now this is populated with hardcoded sample data (see SampleIssues.kt).
 * Once Retrofit is wired in, a mapper will convert the raw GitHub API
 * response into this same shape — the UI layer never needs to change.
 *
 * repoDisplayTitle (v0.39, "CM B") is a computed getter, same pattern
 * as repoFullName below — a humanized version of repoName ("Github
 * Readme Stats" from "github-readme-stats"), used as the primary repo
 * identity on IssueCard, with repoFullName demoted to secondary
 * metadata underneath. See RepoNameFormatting.kt for why this is a
 * deliberate approximation, not the repo's "real" title (GitHub has
 * no such field).
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
    val updatedAtLabel: String,
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
