package io.github.genesjc.opensourcepanda.ui

import androidx.compose.runtime.Composable
import io.github.genesjc.opensourcepanda.R
import io.github.genesjc.opensourcepanda.model.Issue

/**
 * The line under an issue's title: its number, how long ago it was opened and, when GitHub said, by whom
 * ("#89 · opened 22h ago by @sarah_dev"). Null for an issue saved before its number was recorded, where there
 * is nothing true to say. When the opening time is unknown the last update stands in, as the UI layer words it
 * (RelativeTime.kt).
 */
fun StringResolver.issueMeta(issue: Issue, nowMillis: Long = System.currentTimeMillis()): String? {
    if (issue.number <= 0) return null
    val opened = relativeTime(issue.createdAtMs ?: issue.updatedAtMs, nowMillis)
    val author = issue.authorLogin
    return if (author != null) {
        invoke(R.string.issue_meta_opened_by, issue.number, opened, author)
    } else {
        invoke(R.string.issue_meta_opened, issue.number, opened)
    }
}

/** [issueMeta] for a composable, which reads the clock each time it is drawn. */
@Composable
fun issueMetaText(issue: Issue): String? = rememberStringResolver().issueMeta(issue)
