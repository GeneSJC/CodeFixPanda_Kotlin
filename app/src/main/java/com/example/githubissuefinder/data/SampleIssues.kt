package com.example.githubissuefinder.data

import com.example.githubissuefinder.model.Issue
import com.example.githubissuefinder.model.IssueLabel

/**
 * Hand-written data standing in for what a real GitHub Search API response
 * will look like once Retrofit is wired in. Deliberately varied (some
 * bookmarked, some without a language, different label counts) so the
 * card layout gets exercised against realistic edge cases early.
 */
object SampleIssues {

    private val goodFirstIssue = IssueLabel(name = "good first issue", colorHex = "7057FF")
    private val helpWanted = IssueLabel(name = "help wanted", colorHex = "008672")
    private val documentation = IssueLabel(name = "documentation", colorHex = "0075CA")

    val all = listOf(
        Issue(
            id = 1L,
            title = "Add Kotlin coroutine adapter example to docs",
            repoOwner = "square",
            repoName = "retrofit",
            labels = listOf(goodFirstIssue, helpWanted),
            language = "Kotlin",
            commentCount = 4,
            updatedAtLabel = "2d ago",
            isBookmarked = true,
        ),
        Issue(
            id = 2L,
            title = "Improve error message for malformed routes",
            repoOwner = "ktorio",
            repoName = "ktor",
            labels = listOf(goodFirstIssue),
            language = "Kotlin",
            commentCount = 1,
            updatedAtLabel = "5h ago",
        ),
        Issue(
            id = 3L,
            title = "Update README with multiplatform setup instructions",
            repoOwner = "square",
            repoName = "okio",
            labels = listOf(documentation, helpWanted),
            language = "Kotlin",
            commentCount = 7,
            updatedAtLabel = "1d ago",
        ),
        Issue(
            id = 4L,
            title = "Add unit tests for the new caching layer",
            repoOwner = "coil-kt",
            repoName = "coil",
            labels = listOf(goodFirstIssue),
            language = null,
            commentCount = 0,
            updatedAtLabel = "3d ago",
        ),
        Issue(
            id = 5L,
            title = "Clarify contribution guidelines for first-time contributors",
            repoOwner = "cashapp",
            repoName = "turbine",
            labels = listOf(documentation),
            language = "Kotlin",
            commentCount = 2,
            updatedAtLabel = "6d ago",
        ),
    )
}
