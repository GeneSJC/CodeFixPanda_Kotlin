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
        bodyText = body.orEmpty(),
        repoOwner = owner,
        repoName = name,
        labels = labels.map { IssueLabel(name = it.name, colorHex = it.color) },
        language = languageFilter,
        commentCount = comments,
        updatedAtMs = parseGitHubTimestamp(updatedAt),
        htmlUrl = htmlUrl,
    )
}

/**
 * Parses a GitHub ISO 8601 timestamp ("2026-09-10T12:34:56Z") into epoch millis, or null if it cannot
 * be read. Uses SimpleDateFormat because java.time.Instant needs API 26+ (or desugaring) and minSdk
 * is 24. How long ago that was is worded in the UI layer (RelativeTime.kt).
 */
fun parseGitHubTimestamp(isoTimestamp: String): Long? = try {
    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    format.parse(isoTimestamp)?.time
} catch (e: Exception) {
    null
}
