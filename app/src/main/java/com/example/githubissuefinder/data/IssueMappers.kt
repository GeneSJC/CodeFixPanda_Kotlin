package com.example.githubissuefinder.data

import com.example.githubissuefinder.model.Issue
import com.example.githubissuefinder.model.IssueLabel
import com.example.githubissuefinder.network.dto.IssueDto
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Converts a raw API DTO into the Issue domain model. [languageFilter] is passed in because the
 * search endpoint does not return a repo's language; for now the language is simply what was
 * searched for.
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
 * Parses a GitHub ISO 8601 timestamp ("2026-09-10T12:34:56Z") into a relative label. Uses
 * SimpleDateFormat because java.time.Instant needs API 26+ (or desugaring) and minSdk is 24.
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
 * callers that already have one (e.g. RepoIssueGroupRepository's
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
