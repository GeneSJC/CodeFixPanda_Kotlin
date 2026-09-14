package com.example.githubissuefinder.ui.insights

import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.example.githubissuefinder.data.SampleIssues
// Confirmed directly against Drafter's source on GitHub (main branch):
// https://github.com/AndroidPoet/Drafter/blob/main/drafter/src/commonMain/kotlin/io/androidpoet/drafter/treemap/model/TreemapData.kt
// — both TreemapItem and TreemapData are declared in this exact package.
import io.androidpoet.drafter.treemap.model.TreemapData
import io.androidpoet.drafter.treemap.model.TreemapItem
// Confirmed via Android Studio's own successful "Import class" quick-fix
// against the real downloaded artifact (see project history).
import io.androidpoet.drafter.theme.DrafterTheme
// Inferred by convention, NOT independently confirmed: sibling classes to
// the .model package above, one level up. If either of these is wrong,
// Android Studio will flag it — remove the line and use bare TreemapChart /
// TreemapChartRenderer in the code below, then Alt+Enter -> Import class.
import io.androidpoet.drafter.treemap.TreemapChart
import io.androidpoet.drafter.treemap.TreemapChartRenderer

/**
 * A repo "activity landscape," built from fields Issue already has — no
 * model changes, no reshaping of sample data:
 *
 * - Box SIZE = comment count (+1 so zero-comment issues still render a
 *   visible box instead of collapsing to nothing).
 * - Box COLOR = freshness, parsed from updatedAtLabel ("5h ago", "2d ago").
 *   This parsing is a deliberate, temporary stand-in: real freshness should
 *   come from a numeric timestamp once Retrofit replaces SampleIssues.kt —
 *   parsing a *display string* for this is a hack that belongs here only
 *   because that's the only data we currently have.
 *
 * Uses the Drafter charting library (io.github.androidpoet:drafter-android)
 * rather than a hand-built Canvas treemap layout algorithm, per the
 * "leverage well-vetted third-party tools for complex visualizations"
 * project convention established when this screen was scoped.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Insights") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            Text(
                text = "Box size = comment count · color = freshness",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            DrafterTheme(dark = isSystemInDarkTheme()) {
                TreemapChart(
                    renderer = TreemapChartRenderer(
                        TreemapData(
                            items = SampleIssues.all.map { issue ->
                                TreemapItem(
                                    issue.repoFullName,
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
