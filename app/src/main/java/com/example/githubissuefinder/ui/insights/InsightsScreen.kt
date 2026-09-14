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
 * Reflects Feed's live, currently-loaded search results (via
 * CurrentFeedState) instead of SampleIssues.kt directly. Also shows a
 * "sample data" note when Feed itself is in sample-data fallback mode (no
 * GitHub token configured) — CurrentFeedState.state carries that flag
 * through so this screen never silently shows sample-derived numbers as
 * if they were live.
 *
 * Two adjustments were required by real data's shape, both intentional:
 * - LABEL changed from repoFullName to a truncated issue title, since
 *   real results can have several issues from the same repo.
 * - CAPPED to the top 12 issues by comment count, for legibility.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(modifier: Modifier = Modifier) {
    val feedState by CurrentFeedState.state.collectAsStateWithLifecycle()
    val feedIssues = feedState.issues

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
            if (feedState.isSampleData) {
                Text(
                    "Showing sample data — add a GitHub token to local.properties to see live issues.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }

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
