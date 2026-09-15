package com.example.githubissuefinder.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.githubissuefinder.data.CurrentFeedState
import com.example.githubissuefinder.model.Issue
import io.androidpoet.drafter.theme.DrafterTheme
import io.androidpoet.drafter.treemap.TreemapChart
import io.androidpoet.drafter.treemap.TreemapChartRenderer
import io.androidpoet.drafter.treemap.model.TreemapData
import io.androidpoet.drafter.treemap.model.TreemapItem

/**
 * The "Insights" tab's content: a treemap of Feed's current results, plus
 * a freshness legend and a tappable companion list.
 *
 * TAP-TO-NAVIGATE NOTE: Drafter's TreemapChart does not expose a
 * documented click/tap callback for individual segments — it's a single
 * Canvas-drawn composable with no confirmed hit-testing API, and GitHub
 * blocks browsing its source folder directly to verify further. Rather
 * than guess at an unconfirmed parameter (which caused real debugging
 * pain earlier in this project — see v0.7's import saga), tap-to-navigate
 * is implemented as a companion list directly below the treemap instead:
 * same 12 issues, each with a color swatch matching its treemap box
 * color, tappable into Detail. This delivers the actual goal — the
 * treemap area becomes a real way to browse, not just decoration —
 * without depending on an unverified library capability.
 *
 * Everything else (data source, box sizing/coloring, capping at 12,
 * sample-data note) is unchanged from v0.9/v0.10.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(onIssueClick: (issueId: Long) -> Unit, modifier: Modifier = Modifier) {
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
    val brandColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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
            modifier = Modifier.padding(bottom = 8.dp),
        )

        FreshnessLegend(brandColor = brandColor)

        DrafterTheme(dark = isSystemInDarkTheme()) {
            TreemapChart(
                renderer = TreemapChartRenderer(
                    TreemapData(
                        items = topIssues.map { issue ->
                            TreemapItem(
                                truncatedTitle(issue.title),
                                (issue.commentCount + 1).toFloat(),
                                freshnessColor(issue.updatedAtLabel, brandColor),
                            )
                        },
                    ),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text(
            "Tap an issue to open it",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        TreemapCompanionList(
            issues = topIssues,
            brandColor = brandColor,
            onIssueClick = onIssueClick,
        )
    }
}

/** A gradient swatch showing what the treemap's color mapping means: brand color (fresh) fading to gray (stale). */
@Composable
private fun FreshnessLegend(brandColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
    ) {
        Text("Fresh", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Brush.horizontalGradient(listOf(brandColor, Color(0xFFB0B0AA)))),
        )
        Text("Stale", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Companion tappable list — see the TAP-TO-NAVIGATE NOTE above for why this exists instead of clicking the treemap shapes directly. */
@Composable
private fun TreemapCompanionList(
    issues: List<Issue>,
    brandColor: Color,
    onIssueClick: (Long) -> Unit,
) {
    Column {
        issues.forEach { issue ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onIssueClick(issue.id) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(freshnessColor(issue.updatedAtLabel, brandColor)),
                )
                Text(
                    truncatedTitle(issue.title, maxLength = 40),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 10.dp).weight(1f),
                )
                Text(
                    "${issue.commentCount} 💬",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
