package com.example.githubissuefinder.data.demo

import com.example.githubissuefinder.network.dto.IssueDto
import com.example.githubissuefinder.network.dto.LabelDto

/**
 * The fake data the app shows when there is no GitHub token: three repos and a handful of issues,
 * every title starting "[Demo]". Written as the same DTOs GitHub returns, so the real mapping code
 * handles it. Ids are negative, which no GitHub issue id ever is, so a demo issue can never be
 * mistaken for a real one.
 */
internal object DemoData {

    private const val OWNER = "demo-org"
    private const val GOOD_FIRST_ISSUE = "good first issue"
    private const val HELP_WANTED = "help wanted"

    /** A fake issue and the language of its repo, which the Search API would match on and the DTO does not carry. */
    class DemoIssue(val dto: IssueDto, val repoFullName: String, val language: String)

    private var nextId = -1L

    private fun issue(
        repo: String,
        language: String,
        number: Int,
        title: String,
        updatedAt: String,
        comments: Int,
        vararg labels: String,
    ): DemoIssue {
        val dto = IssueDto(
            id = nextId--,
            number = number,
            title = "[Demo] $title",
            body = "This is demo data, shown because no GitHub token is set. Add a token in Settings to see real issues.",
            state = "open",
            htmlUrl = "https://github.com/$OWNER/$repo/issues/$number",
            repositoryUrl = "https://api.github.com/repos/$OWNER/$repo",
            comments = comments,
            updatedAt = updatedAt,
            labels = labels.map { LabelDto(name = it, color = if (it == GOOD_FIRST_ISSUE) "7057ff" else "008672") },
        )
        return DemoIssue(dto, repoFullName = "$OWNER/$repo", language = language)
    }

    val issues: List<DemoIssue> = listOf(
        issue("demo-tasks-app", "kotlin", 12, "Add a dark theme toggle to the settings screen", "2026-10-14T09:30:00Z", 2, GOOD_FIRST_ISSUE),
        issue("demo-tasks-app", "kotlin", 15, "Fix the crash when the task list is empty", "2026-10-12T16:05:00Z", 4, GOOD_FIRST_ISSUE),
        issue("demo-tasks-app", "kotlin", 19, "Document the build steps in the README", "2026-10-09T11:20:00Z", 0, GOOD_FIRST_ISSUE),
        issue("demo-tasks-app", "kotlin", 21, "Add a unit test for the due-date parser", "2026-10-05T08:45:00Z", 1, GOOD_FIRST_ISSUE),
        issue("demo-tasks-app", "kotlin", 27, "Support swipe-to-archive on a task", "2026-10-13T14:10:00Z", 6, HELP_WANTED),
        issue("demo-tasks-app", "kotlin", 30, "Improve the empty-state illustration", "2026-10-02T19:00:00Z", 3, HELP_WANTED),
        issue("demo-api-server", "java", 7, "Return 404 instead of 500 for unknown ids", "2026-10-15T07:40:00Z", 1, GOOD_FIRST_ISSUE),
        issue("demo-api-server", "java", 11, "Clarify the error messages in the validator", "2026-10-10T13:25:00Z", 2, GOOD_FIRST_ISSUE),
        issue("demo-api-server", "java", 14, "Add pagination to the /tasks endpoint", "2026-10-11T10:15:00Z", 5, HELP_WANTED),
        issue("demo-api-server", "java", 18, "Add request logging", "2026-10-06T15:50:00Z", 0, HELP_WANTED),
        issue("demo-api-server", "java", 22, "Upgrade the HTTP client dependency", "2026-10-01T09:00:00Z", 2, HELP_WANTED),
        issue("demo-data-tools", "python", 3, "Fix the typo in the CSV export header", "2026-10-16T12:00:00Z", 0, GOOD_FIRST_ISSUE),
        issue("demo-data-tools", "python", 9, "Add type hints to the parser module", "2026-10-08T17:30:00Z", 1, HELP_WANTED),
    )
}
