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
import androidx.compose.material3.Text
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
 * The "Insights" tab's content: a treemap of Feed's current results. No
 * longer owns its own Scaffold or TopAppBar — as of this Commit Milestone
 * it's hosted inside FeedTabsScreen alongside FeedScreen ("List"), sharing
 * one TopAppBar and a top TabRow between them. Insights was previously its
 * own bottom-nav destination; merged here because it was never
 * conceptually a separate place, just a different lens on the same
 * current search results (it has always sourced from CurrentFeedState,
 * which Feed populates — no data-flow change here, only where this
 * content is hosted).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(modifier: Modifier = Modifier) {
    val feedState by CurrentFeedState.state.collectAsStateWithLifecycle()
    val feedIssues = feedState.issues

    if (feedIssues.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "No data yet — switch to the List tab to load issues.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    val topIssues = feedIssues.sortedByDescending { it.commentCount }.take(12)

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
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
