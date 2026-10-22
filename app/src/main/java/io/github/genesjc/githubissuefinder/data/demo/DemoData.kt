package io.github.genesjc.githubissuefinder.data.demo

import io.github.genesjc.githubissuefinder.network.dto.IssueDto
import io.github.genesjc.githubissuefinder.network.dto.LabelDto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * The fake data the app shows when there is no GitHub token: [repos], sixteen fictional repositories
 * (three or four for each language the filter offers), each with a few issues in both modes, so every
 * language and mode shows something. Every title starts "[Demo]". Written as the same DTOs GitHub
 * returns, so the real mapping code handles it. Ids are negative, which no GitHub issue id ever is, so a
 * demo issue can never be mistaken for a real one. Update times are set relative to now, so the lists
 * never look stale. The repositories and issues are invented, not copied from anywhere.
 */
internal object DemoData {

    private const val OWNER = "demo-org"
    private const val GOOD_FIRST_ISSUE = "good first issue"
    private const val HELP_WANTED = "help wanted"

    /** A fake repository: its name, its language (as the Search API's `language:` qualifier spells it), and a one-line description for its README. */
    class DemoRepo(val name: String, val language: String, val description: String)

    /** A fake issue and the language of its repo, which the Search API would match on and the DTO does not carry. */
    class DemoIssue(val dto: IssueDto, val repoFullName: String, val language: String)

    val repos: List<DemoRepo> = listOf(
        DemoRepo("demo-tasks-app", "kotlin", "A small task manager for Android, written with Jetpack Compose."),
        DemoRepo("demo-weather-widget", "kotlin", "A home-screen weather widget for Android with a tiny footprint."),
        DemoRepo("demo-recipe-box", "kotlin", "Keep, scale and share your recipes. A Kotlin sample app."),
        DemoRepo("demo-habit-tracker", "kotlin", "Track daily habits with streaks and gentle reminders."),
        DemoRepo("demo-api-server", "java", "A small REST API for tasks, with validation and clear error messages."),
        DemoRepo("demo-inventory-service", "java", "A stock-keeping service with a REST API and a simple admin page."),
        DemoRepo("demo-chat-gateway", "java", "A WebSocket gateway that fans messages out to chat rooms."),
        DemoRepo("demo-pocket-notes", "swift", "A quick-capture notes app for iPhone, built with SwiftUI."),
        DemoRepo("demo-run-log", "swift", "Log your runs and see weekly distance charts."),
        DemoRepo("demo-photo-grid", "swift", "A fast, memory-friendly photo grid for large libraries."),
        DemoRepo("demo-web-dashboard", "javascript", "A React dashboard that turns CSV files into live charts."),
        DemoRepo("demo-markdown-editor", "javascript", "A lightweight Markdown editor with a live preview."),
        DemoRepo("demo-cli-toolkit", "javascript", "A Node command-line toolkit for everyday file chores."),
        DemoRepo("demo-data-tools", "python", "Small helpers for cleaning, converting and exporting CSV data."),
        DemoRepo("demo-image-resizer", "python", "Batch-resize and convert images from the command line."),
        DemoRepo("demo-bot-framework", "python", "A small framework for writing chat bots in Python."),
    )

    private var nextId = -1L

    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /** An ISO timestamp [days] days before now, the shape GitHub sends. */
    private fun daysAgo(days: Int): String = timestampFormat.format(Date(System.currentTimeMillis() - days * 86_400_000L))

    private fun issue(
        repo: String,
        number: Int,
        title: String,
        updatedDaysAgo: Int,
        comments: Int,
        label: String,
    ): DemoIssue {
        val language = repos.first { it.name == repo }.language
        val dto = IssueDto(
            id = nextId--,
            number = number,
            title = "[Demo] $title",
            body = "This is demo data, shown because no GitHub token is set. Add a token in Settings to see real issues.",
            state = "open",
            htmlUrl = "https://github.com/$OWNER/$repo/issues/$number",
            repositoryUrl = "https://api.github.com/repos/$OWNER/$repo",
            comments = comments,
            updatedAt = daysAgo(updatedDaysAgo),
            labels = listOf(LabelDto(name = label, color = if (label == GOOD_FIRST_ISSUE) "7057ff" else "008672")),
        )
        return DemoIssue(dto, repoFullName = "$OWNER/$repo", language = language)
    }

    val issues: List<DemoIssue> = listOf(
        issue("demo-tasks-app", 12, "Add a dark theme toggle to the settings screen", 8, 2, GOOD_FIRST_ISSUE),
        issue("demo-tasks-app", 15, "Fix the crash when the task list is empty", 10, 4, GOOD_FIRST_ISSUE),
        issue("demo-tasks-app", 19, "Document the build steps in the README", 13, 0, GOOD_FIRST_ISSUE),
        issue("demo-tasks-app", 21, "Add a unit test for the due-date parser", 17, 1, GOOD_FIRST_ISSUE),
        issue("demo-tasks-app", 27, "Support swipe-to-archive on a task", 9, 6, HELP_WANTED),
        issue("demo-tasks-app", 30, "Improve the empty-state illustration", 20, 3, HELP_WANTED),
        issue("demo-tasks-app", 33, "Show an error message when saving fails", 3, 1, GOOD_FIRST_ISSUE),
        issue("demo-tasks-app", 36, "Add a widget that lists today's tasks", 5, 7, HELP_WANTED),
        issue("demo-weather-widget", 4, "Fix the wrong icon for light rain", 2, 1, GOOD_FIRST_ISSUE),
        issue("demo-weather-widget", 8, "Show the temperature unit setting in the widget", 6, 3, GOOD_FIRST_ISSUE),
        issue("demo-weather-widget", 11, "Add a content description to the refresh button", 12, 0, GOOD_FIRST_ISSUE),
        issue("demo-weather-widget", 14, "Cache the last forecast for offline use", 4, 5, HELP_WANTED),
        issue("demo-weather-widget", 17, "Support a two-by-one widget size", 15, 2, HELP_WANTED),
        issue("demo-weather-widget", 20, "Translate the strings into Spanish", 22, 4, HELP_WANTED),
        issue("demo-recipe-box", 5, "Round servings to a sensible fraction", 1, 2, GOOD_FIRST_ISSUE),
        issue("demo-recipe-box", 9, "Add a unit test for the ingredient scaler", 7, 0, GOOD_FIRST_ISSUE),
        issue("demo-recipe-box", 12, "Fix the cut-off title on small screens", 11, 1, GOOD_FIRST_ISSUE),
        issue("demo-recipe-box", 16, "Import a recipe from a web page", 3, 9, HELP_WANTED),
        issue("demo-recipe-box", 23, "Let a recipe have more than one photo", 19, 4, HELP_WANTED),
        issue("demo-habit-tracker", 3, "Show the streak count on the habit card", 5, 2, GOOD_FIRST_ISSUE),
        issue("demo-habit-tracker", 6, "Use a plural string for \"1 day\" and \"2 days\"", 9, 1, GOOD_FIRST_ISSUE),
        issue("demo-habit-tracker", 10, "Remind on selected weekdays only", 2, 6, HELP_WANTED),
        issue("demo-habit-tracker", 13, "Export habits to a CSV file", 14, 3, HELP_WANTED),
        issue("demo-api-server", 7, "Return 404 instead of 500 for unknown ids", 3, 1, GOOD_FIRST_ISSUE),
        issue("demo-api-server", 11, "Clarify the error messages in the validator", 8, 2, GOOD_FIRST_ISSUE),
        issue("demo-api-server", 14, "Add pagination to the /tasks endpoint", 6, 5, HELP_WANTED),
        issue("demo-api-server", 18, "Add request logging", 11, 0, HELP_WANTED),
        issue("demo-api-server", 22, "Upgrade the HTTP client dependency", 16, 2, HELP_WANTED),
        issue("demo-api-server", 25, "Add an example request to the API docs", 4, 1, GOOD_FIRST_ISSUE),
        issue("demo-api-server", 28, "Rate-limit the login endpoint", 9, 8, HELP_WANTED),
        issue("demo-inventory-service", 6, "Reject negative stock quantities", 1, 0, GOOD_FIRST_ISSUE),
        issue("demo-inventory-service", 9, "Fix the typo in the low-stock email", 6, 1, GOOD_FIRST_ISSUE),
        issue("demo-inventory-service", 13, "Add a Dockerfile for local development", 10, 3, GOOD_FIRST_ISSUE),
        issue("demo-inventory-service", 17, "Add search by SKU prefix", 3, 4, HELP_WANTED),
        issue("demo-inventory-service", 21, "Write integration tests for the restock job", 13, 2, HELP_WANTED),
        issue("demo-inventory-service", 24, "Support barcode lookups", 18, 5, HELP_WANTED),
        issue("demo-chat-gateway", 2, "Close idle connections after a timeout", 4, 3, HELP_WANTED),
        issue("demo-chat-gateway", 5, "Log the reason when a client disconnects", 7, 1, GOOD_FIRST_ISSUE),
        issue("demo-chat-gateway", 8, "Document the message format", 12, 0, GOOD_FIRST_ISSUE),
        issue("demo-chat-gateway", 12, "Add a health-check endpoint", 2, 2, GOOD_FIRST_ISSUE),
        issue("demo-chat-gateway", 15, "Reconnect with backoff in the sample client", 9, 6, HELP_WANTED),
        issue("demo-pocket-notes", 4, "Fix the keyboard covering the last line", 3, 2, GOOD_FIRST_ISSUE),
        issue("demo-pocket-notes", 7, "Add VoiceOver labels to the toolbar", 8, 1, GOOD_FIRST_ISSUE),
        issue("demo-pocket-notes", 10, "Pin a note to the top of the list", 5, 4, HELP_WANTED),
        issue("demo-pocket-notes", 14, "Add a widget for quick capture", 11, 7, HELP_WANTED),
        issue("demo-pocket-notes", 18, "Sort notes by last edited", 15, 0, GOOD_FIRST_ISSUE),
        issue("demo-run-log", 3, "Show pace in min/km and min/mile", 2, 3, GOOD_FIRST_ISSUE),
        issue("demo-run-log", 6, "Fix the weekly chart's first day of the week", 6, 2, GOOD_FIRST_ISSUE),
        issue("demo-run-log", 9, "Add a unit test for the distance formatter", 10, 0, GOOD_FIRST_ISSUE),
        issue("demo-run-log", 12, "Import runs from a GPX file", 4, 5, HELP_WANTED),
        issue("demo-run-log", 16, "Draw the route on a map", 14, 8, HELP_WANTED),
        issue("demo-photo-grid", 5, "Reduce the memory use of the thumbnail cache", 3, 4, HELP_WANTED),
        issue("demo-photo-grid", 8, "Add an accessibility label to each photo", 9, 1, GOOD_FIRST_ISSUE),
        issue("demo-photo-grid", 11, "Fix the stutter when scrolling fast", 6, 6, HELP_WANTED),
        issue("demo-photo-grid", 13, "Add a README section on building the demo app", 13, 0, GOOD_FIRST_ISSUE),
        issue("demo-web-dashboard", 14, "Format large numbers with thousands separators", 2, 1, GOOD_FIRST_ISSUE),
        issue("demo-web-dashboard", 18, "Fix the legend overlapping the chart", 5, 3, GOOD_FIRST_ISSUE),
        issue("demo-web-dashboard", 22, "Add keyboard navigation to the chart tooltip", 8, 2, HELP_WANTED),
        issue("demo-web-dashboard", 27, "Support a dark mode", 4, 9, HELP_WANTED),
        issue("demo-web-dashboard", 31, "Add a test for the CSV parser", 12, 0, GOOD_FIRST_ISSUE),
        issue("demo-web-dashboard", 33, "Load large files in chunks", 16, 6, HELP_WANTED),
        issue("demo-markdown-editor", 5, "Support task lists in the preview", 3, 4, HELP_WANTED),
        issue("demo-markdown-editor", 8, "Fix the scroll sync on long documents", 7, 5, HELP_WANTED),
        issue("demo-markdown-editor", 11, "Add a keyboard shortcut for bold", 1, 1, GOOD_FIRST_ISSUE),
        issue("demo-markdown-editor", 15, "Fix the typo in the toolbar tooltip", 10, 0, GOOD_FIRST_ISSUE),
        issue("demo-markdown-editor", 19, "Document how to embed the editor", 14, 2, GOOD_FIRST_ISSUE),
        issue("demo-cli-toolkit", 4, "Print a friendly error when a path does not exist", 2, 2, GOOD_FIRST_ISSUE),
        issue("demo-cli-toolkit", 7, "Add a --dry-run flag to the rename command", 5, 3, HELP_WANTED),
        issue("demo-cli-toolkit", 10, "Colour the output only when it is a terminal", 9, 1, GOOD_FIRST_ISSUE),
        issue("demo-cli-toolkit", 13, "Add shell completion for the bash shell", 6, 4, HELP_WANTED),
        issue("demo-data-tools", 3, "Fix the typo in the CSV export header", 4, 0, GOOD_FIRST_ISSUE),
        issue("demo-data-tools", 9, "Add type hints to the parser module", 11, 1, HELP_WANTED),
        issue("demo-data-tools", 12, "Add a --delimiter option to the CSV reader", 2, 3, HELP_WANTED),
        issue("demo-data-tools", 15, "Explain the date format in the docstring", 7, 0, GOOD_FIRST_ISSUE),
        issue("demo-data-tools", 18, "Handle empty files without a traceback", 5, 2, GOOD_FIRST_ISSUE),
        issue("demo-image-resizer", 4, "Keep the EXIF orientation when resizing", 1, 3, HELP_WANTED),
        issue("demo-image-resizer", 7, "Fix the typo in the --help text", 8, 0, GOOD_FIRST_ISSUE),
        issue("demo-image-resizer", 10, "Add a progress bar for large batches", 4, 5, HELP_WANTED),
        issue("demo-image-resizer", 14, "Add tests for the filename template", 12, 1, GOOD_FIRST_ISSUE),
        issue("demo-image-resizer", 17, "Support WebP output", 6, 2, HELP_WANTED),
        issue("demo-bot-framework", 6, "Add an example bot to the README", 3, 2, GOOD_FIRST_ISSUE),
        issue("demo-bot-framework", 9, "Raise a clear error for a missing token", 7, 1, GOOD_FIRST_ISSUE),
        issue("demo-bot-framework", 12, "Retry failed sends with backoff", 2, 6, HELP_WANTED),
        issue("demo-bot-framework", 15, "Add type hints to the command decorators", 10, 0, HELP_WANTED),
        issue("demo-bot-framework", 19, "Document how to write a plugin", 13, 3, GOOD_FIRST_ISSUE),
    )
}
