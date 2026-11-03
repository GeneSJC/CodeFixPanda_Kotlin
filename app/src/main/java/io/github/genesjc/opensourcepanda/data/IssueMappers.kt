package io.github.genesjc.opensourcepanda.data

import io.github.genesjc.opensourcepanda.data.network.dto.IssueDto
import io.github.genesjc.opensourcepanda.model.DataOrigin
import io.github.genesjc.opensourcepanda.model.Issue
import io.github.genesjc.opensourcepanda.model.IssueLabel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Converts a raw API DTO into the Issue domain model. [languageFilter] is passed in because the
 * search endpoint does not return a repo's language; for now the language is simply what was
 * searched for. [origin] marks where the issue came from (see Issue.origin).
 */
fun IssueDto.toIssue(languageFilter: String?, origin: DataOrigin = DataOrigin.GITHUB): Issue {
    val segments = repositoryUrl.trimEnd('/').split("/")
    val owner = segments.getOrNull(segments.size - 2)?.takeIf { it.isNotBlank() }
    val name = segments.getOrNull(segments.size - 1)?.takeIf { it.isNotBlank() }
    // GitHub always sends a repository URL. An item without a usable one cannot be shown, and the search then
    // fails with this message instead of listing an issue under an empty repo name.
    checkNotNull(owner) { "Issue $id has no repository owner in its repository_url: $repositoryUrl" }
    checkNotNull(name) { "Issue $id has no repository name in its repository_url: $repositoryUrl" }

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
        origin = origin,
        number = number,
        createdAtMs = createdAt?.let(::parseGitHubTimestamp),
        authorLogin = user?.login,
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
