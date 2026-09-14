package com.example.githubissuefinder.ui.insights

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.githubissuefinder.data.CurrentFeedState
import io.androidpoet.drafter.theme.DrafterTheme
import io.androidpoet.drafter.treemap.TreemapChart
import io.androidpoet.drafter.treemap.TreemapChartRenderer
import io.androidpoet.drafter.treemap.model.TreemapData
import io.androidpoet.drafter.treemap.model.TreemapItem

/**
 * Now reflects Feed's live, currently-loaded search results (via
 * CurrentFeedState) instead of SampleIssues.kt. Two adjustments were
 * required by real data's shape, both intentional:
 *
 * - LABEL changed from repoFullName to a truncated issue title. Sample
 *   data had exactly one issue per repo, so repoFullName was a unique,
 *   meaningful label per box. Real search results can return several
 *   issues from the same repo (visible in the Feed screenshots — multiple
 *   oppia/oppia-android issues at once), which would produce confusing
 *   duplicate box labels if we kept repoFullName.
 * - CAPPED to the top 12 issues by comment count. GitHub search can
 *   return up to 30 results per page; a 30-box treemap would be
 *   unreadable on a phone screen. This is a deliberate legibility
 *   decision, not an arbitrary limitation — the most-discussed issues
 *   are also arguably the most interesting ones to surface here.
 *
 * Box SIZE (comment count) and COLOR (freshness) logic are unchanged from
 * the sample-data version. One known minor imprecision: real data's
 * relative-time formatter (IssueMappers.kt) can also produce "just now"
 * or "recently", which don't match this screen's \d+[hd] regex and fall
 * back to a fixed 24h-equivalent freshness — visually harmless, not a bug.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(modifier: Modifier = Modifier) {
    val feedIssues by CurrentFeedState.issues.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Insights") }) },
    ) { padding ->
        if (feedIssues.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "No data yet — visit the Feed tab to load issues.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            return@Scaffold
        }

        val topIssues = feedIssues.sortedByDescending { it.commentCount }.take(12)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            Text(
                text = "Box size = comments · color = freshness · " +
                    "top ${topIssues.size} of ${feedIssues.size} current results",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            DrafterTheme(dark = isSystemInDarkTheme()) {
                TreemapChart(
                    renderer = TreemapChartRenderer(
                        TreemapData(
                            items = topIssues.map { issue ->
                                TreemapItem(
                                    truncatedTitle(issue.title),
                                    (issue.commentCount + 1).toFloat(),
                                    freshnessColor(issue.updatedAtLabel, MaterialTheme.colorScheme.primary),
                                )
                            },
                        ),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                )
            }
        }
    }
}

private fun truncatedTitle(title: String, maxLength: Int = 28): String =
    if (title.length > maxLength) "${title.take(maxLength)}…" else title

/**
 * Blends from the app's brand primary color (fresh) toward a neutral gray
 * (stale) based on an approximate hours-since-update value parsed from a
 * display string. Anything a week or older reads as fully "cold."
 */
private fun freshnessColor(updatedAtLabel: String, brandColor: Color): Color {
    val hoursAgo = parseHoursAgo(updatedAtLabel)
    val staleness = (hoursAgo / (24f * 7f)).coerceIn(0f, 1f)
    return lerp(brandColor, Color(0xFFB0B0AA), staleness)
}

private fun parseHoursAgo(label: String): Float {
    val match = Regex("""(\d+)([hd])""").find(label) ?: return 24f
    val (amount, unit) = match.destructured
    val value = amount.toFloatOrNull() ?: return 24f
    return if (unit == "d") value * 24f else value
}
