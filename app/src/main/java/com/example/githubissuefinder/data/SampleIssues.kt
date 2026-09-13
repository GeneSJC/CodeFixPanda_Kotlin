package com.example.githubissuefinder.data

import com.example.githubissuefinder.model.Issue
import com.example.githubissuefinder.model.IssueLabel

/**
 * Hand-written data standing in for what a real GitHub Search API response
 * will look like once Retrofit is wired in.
 */
object SampleIssues {

    private val goodFirstIssue = IssueLabel(name = "good first issue", colorHex = "7057FF")
    private val helpWanted = IssueLabel(name = "help wanted", colorHex = "008672")
    private val documentation = IssueLabel(name = "documentation", colorHex = "0075CA")

    val all = listOf(
        Issue(
            id = 1L,
            title = "Add Kotlin coroutine adapter example to docs",
            bodyText = "The retrofit adapter docs only show a Java callback example. It would help new contributors to add a Kotlin suspend fun example alongside it. Should live in docs/kotlin.md.",
            repoOwner = "square",
            repoName = "retrofit",
            labels = listOf(goodFirstIssue, helpWanted),
            language = "Kotlin",
            commentCount = 4,
            updatedAtLabel = "2d ago",
        ),
        Issue(
            id = 2L,
            title = "Improve error message for malformed routes",
            bodyText = "When a route pattern is malformed, the current exception message doesn't point to the offending path segment. A clearer message would save people a lot of debugging time.",
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
            bodyText = "The README covers JVM setup well but multiplatform (iOS/JS targets) isn't documented at all. A short getting-started section would help a lot of newcomers.",
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
            bodyText = "The disk cache eviction logic added last month has no test coverage yet. Good opportunity to get familiar with the caching module.",
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
            bodyText = "CONTRIBUTING.md assumes familiarity with the project's branching model. Adding a short 'first PR' walkthrough would lower the barrier for newcomers.",
            repoOwner = "cashapp",
            repoName = "turbine",
            labels = listOf(documentation),
            language = "Kotlin",
            commentCount = 2,
            updatedAtLabel = "6d ago",
        ),
    )
}
