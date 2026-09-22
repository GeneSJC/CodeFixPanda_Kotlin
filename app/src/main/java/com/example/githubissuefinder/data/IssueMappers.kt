package com.example.githubissuefinder.data

import com.example.githubissuefinder.model.Issue
import com.example.githubissuefinder.model.IssueLabel
import com.example.githubissuefinder.network.dto.IssueDto
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Converts a raw API DTO into our existing Issue domain model. Deliberately
 * produces the exact same shape (and the same "Xh ago" / "Xd ago" string
 * format for updatedAtLabel) that SampleIssues.kt always used — this means
 * IssueCard, DetailScreen, BookmarksScreen, and LabelChip needed ZERO
 * changes to work with real data instead of sample data.
 *
 * Note: IssueDto has a "number" field (GitHub's per-repo issue number),
 * but Issue (this app's domain model) does not — that field was only ever
 * needed by the shelved Detail-screen Comments-tab work, which was never
 * applied to this project. Left unmapped here on purpose; do not add it
 * back without confirming Issue.kt actually has a matching field first.
 *
 * languageFilter is passed in rather than read from the API response,
 * because GitHub's search endpoint doesn't return a repo's language
 * directly — that requires a separate per-repo call we're not making yet.
 * For now, language is simply "whatever you searched for." Real per-repo
 * language enrichment is a reasonable follow-up Commit Milestone.
 */
fun IssueDto.toIssue(languageFilter: String?): Issue {
    val segments = repositoryUrl.trimEnd('/').split("/")
    val owner = segments.getOrElse(segments.size - 2) { "" }
    val name = segments.getOrElse(segments.size - 1) { "" }

    return Issue(
        id = id,
        title = title,
        bodyText = body?.takeIf { it.isNotBlank() } ?: "No description provided.",
        repoOwner = owner,
        repoName = name,
        labels = labels.map { IssueLabel(name = it.name, colorHex = it.color) },
        language = languageFilter,
        commentCount = comments,
        updatedAtLabel = formatRelativeTime(updatedAt),
        htmlUrl = htmlUrl,
    )
}

/**
 * Parses a GitHub-format ISO 8601 timestamp ("2026-09-10T12:34:56Z") into
 * a relative label. Uses SimpleDateFormat rather than java.time, since
 * java.time.Instant requires API 26+ (or core library desugaring, which
 * we haven't set up) and this project's minSdk is 24.
 *
 * Public (not private) so other files in this package — currently
 * RepoScoring.kt (v0.26) — can reuse the exact same formatting. In
 * Kotlin, a top-level `private` function is scoped to its FILE, not its
 * package (unlike Java's package-private default), so this couldn't stay
 * private once a second file needed it, even within the same package.
 */
fun formatRelativeTime(isoTimestamp: String): String {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val thenMillis = format.parse(isoTimestamp)?.time ?: return "recently"
        formatRelativeTime(thenMillis)
    } catch (e: Exception) {
        "recently"
    }
}

/**
 * v0.45: overload taking a plain epoch-millis timestamp directly, for
 * callers that already have one (e.g. RepoIssueGroupCache's
 * fetchedAtMs, used for Top Repos' "Updated X ago" label) rather than
 * a GitHub-format ISO string to parse first. The ISO-string overload
 * above now delegates here once it's parsed the timestamp, instead of
 * duplicating the hoursAgo bucketing logic in two places.
 */
fun formatRelativeTime(thenMillis: Long): String {
    val hoursAgo = (System.currentTimeMillis() - thenMillis) / (1000 * 60 * 60)
    return when {
        hoursAgo < 1 -> "just now"
        hoursAgo < 24 -> "${hoursAgo}h ago"
        else -> "${hoursAgo / 24}d ago"
    }
}
