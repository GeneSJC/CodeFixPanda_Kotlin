package com.example.githubissuefinder.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import io.androidpoet.drafter.bars.BarChart
import io.androidpoet.drafter.bars.model.SimpleBarChartData
import io.androidpoet.drafter.bars.renderer.BarChartRenderer
import io.androidpoet.drafter.pie.PieChart
import io.androidpoet.drafter.pie.model.PieChartData
import io.androidpoet.drafter.pie.renderer.DonutChartRenderer
import io.androidpoet.drafter.scatterplot.ScatterPlot
import io.androidpoet.drafter.scatterplot.SimpleScatterPlotRenderer
import io.androidpoet.drafter.scatterplot.model.ScatterPlotData
import io.androidpoet.drafter.theme.DrafterTheme
import io.androidpoet.drafter.treemap.TreemapChart
import io.androidpoet.drafter.treemap.TreemapChartRenderer
import io.androidpoet.drafter.treemap.model.TreemapData
import io.androidpoet.drafter.treemap.model.TreemapItem
// NOTE: PieChart, DonutChartRenderer, PieChartData, BarChart,
// BarChartRenderer, SimpleBarChartData, ScatterPlot,
// SimpleScatterPlotRenderer, and ScatterPlotData are intentionally NOT
// imported here. Their constructor shapes are confirmed directly from
// Drafter's official README (not guessed), but their package paths are
// not — same situation the Treemap classes were originally in. Rather
// than guess a path again (which caused real debugging pain in v0.7),
// see README.txt for the Android Studio quick-fix steps that resolve
// these against the real downloaded artifact.

/**
 * The "Insights" tab's content. Now offers four visualization modes via a
 * simple text-chip picker, per the "start with a text list" instruction
 * from the UI/UX roadmap discussion — a richer thumbnail/carousel picker
 * is a deliberately separate future Commit Milestone (item 3b), not
 * bundled into this one.
 *
 * - Treemap: unchanged from v0.14 (box size = comments, color =
 *   freshness, companion tappable list below).
 * - Donut: LABEL OVERLAP, not "label distribution" — see the long
 *   comment on buildLabelOverlapSlices() for why those aren't the same
 *   thing given how Feed's search filtering works.
 * - Bar: issue count per repo, top 8 by count for legibility.
 * - Scatter: comment count (y) vs. hours-since-update (x) per issue,
 *   colored using the same freshness gradient as the treemap — the most
 *   analytically interesting view, since it can reveal "hot right now"
 *   issues a treemap alone can't show.
 *
 * The freshness legend only appears for Treemap/Scatter, since Donut and
 * Bar use a fixed categorical palette, not a freshness gradient — showing
 * the legend there would be actively misleading, not just unnecessary.
 */
private enum class InsightChartType(val label: String) {
    TREEMAP("Treemap"),
    DONUT("Donut"),
    BAR("Bar"),
    SCATTER("Scatter"),
}

private val CategoricalPalette = listOf(
    Color(0xFF0F6E56),
    Color(0xFF534AB7),
    Color(0xFF0B5FA5),
    Color(0xFFC77B00),
    Color(0xFFB3435C),
    Color(0xFF2E8B7A),
    Color(0xFF7057FF),
    Color(0xFF008672),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(onIssueClick: (issueId: Long) -> Unit, modifier: Modifier = Modifier) {
    val feedState by CurrentFeedState.state.collectAsStateWithLifecycle()
    val feedIssues = feedState.issues
    var selectedChart by remember { mutableStateOf(InsightChartType.TREEMAP) }

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

    val topIssuesByComments = feedIssues.sortedByDescending { it.commentCount }.take(12)
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

        ChartPicker(selected = selectedChart, onSelect = { selectedChart = it })

        Text(
            text = captionFor(selectedChart, topIssuesByComments.size, feedIssues.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp),
        )

        if (selectedChart == InsightChartType.TREEMAP || selectedChart == InsightChartType.SCATTER) {
            FreshnessLegend(brandColor = brandColor)
        }

        when (selectedChart) {
            InsightChartType.TREEMAP -> TreemapSection(topIssuesByComments, brandColor)
            InsightChartType.DONUT -> DonutSection(feedIssues)
            InsightChartType.BAR -> BarSection(feedIssues)
            InsightChartType.SCATTER -> ScatterSection(feedIssues, brandColor)
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text(
            "Tap an issue to open it",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        TreemapCompanionList(
            issues = topIssuesByComments,
            brandColor = brandColor,
            onIssueClick = onIssueClick,
        )
    }
}

private fun captionFor(chart: InsightChartType, shown: Int, total: Int): String = when (chart) {
    InsightChartType.TREEMAP -> "Box size = comments · color = freshness · top $shown of $total current results"
    InsightChartType.DONUT -> "Label overlap across $total current results"
    InsightChartType.BAR -> "Issues per repo · top repos of $total current results"
    InsightChartType.SCATTER -> "Comments vs. freshness · $total current results"
}

@Composable
private fun ChartPicker(selected: InsightChartType, onSelect: (InsightChartType) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        items(InsightChartType.entries) { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(option.label) },
            )
        }
    }
}

// ---------------------------------------------------------------------
// Treemap (unchanged from v0.14)
// ---------------------------------------------------------------------

@Composable
private fun TreemapSection(issues: List<Issue>, brandColor: Color) {
    DrafterTheme(dark = isSystemInDarkTheme()) {
        TreemapChart(
            renderer = TreemapChartRenderer(
                TreemapData(
                    items = issues.map { issue ->
                        TreemapItem(
                            truncatedTitle(issue.title),
                            (issue.commentCount + 1).toFloat(),
                            freshnessColor(issue.updatedAtLabel, brandColor),
                        )
                    },
                ),
            ),
            modifier = Modifier.fillMaxWidth().height(320.dp),
        )
    }
}

// ---------------------------------------------------------------------
// Donut — label overlap, not "label distribution"
// ---------------------------------------------------------------------

/**
 * Feed's search always filters server-side by ONE label qualifier
 * (good first issue OR help wanted) — so every result already,
 * guaranteed, carries that label. A true "distribution" chart would
 * therefore always show one 100% slice, which isn't an interesting or
 * honest visualization. Instead, this buckets by which OTHER label (if
 * any) each issue also carries — a genuinely variable, honest thing to
 * show regardless of which single label is currently selected in Feed.
 */
private fun buildLabelOverlapSlices(issues: List<Issue>): List<Pair<String, Int>> {
    var both = 0
    var goodOnly = 0
    var helpOnly = 0
    issues.forEach { issue ->
        val hasGood = issue.labels.any { it.name.equals("good first issue", ignoreCase = true) }
        val hasHelp = issue.labels.any { it.name.equals("help wanted", ignoreCase = true) }
        when {
            hasGood && hasHelp -> both++
            hasGood -> goodOnly++
            hasHelp -> helpOnly++
        }
    }
    return listOfNotNull(
        ("Both labels" to both).takeIf { both > 0 },
        ("Good first issue only" to goodOnly).takeIf { goodOnly > 0 },
        ("Help wanted only" to helpOnly).takeIf { helpOnly > 0 },
    )
}

@Composable
private fun DonutSection(issues: List<Issue>) {
    val slices = buildLabelOverlapSlices(issues)
    if (slices.isEmpty()) {
        Text(
            "Not enough label data to show this chart.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    DrafterTheme(dark = isSystemInDarkTheme()) {
        PieChart(
            renderer = DonutChartRenderer(
                PieChartData(
                    slices = slices.mapIndexed { index, (label, count) ->
                        PieChartData.Slice(
                            value = count.toFloat(),
                            color = CategoricalPalette[index % CategoricalPalette.size],
                            label = label,
                        )
                    },
                ),
            ),
            modifier = Modifier.size(220.dp),
            animate = true,
        )
    }

    Column(modifier = Modifier.padding(top = 12.dp)) {
        slices.forEachIndexed { index, (label, count) ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(CategoricalPalette[index % CategoricalPalette.size]),
                )
                Text(
                    "$label ($count)",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------
// Bar — issue count per repo
// ---------------------------------------------------------------------

private fun buildRepoCounts(issues: List<Issue>, topN: Int = 8): List<Pair<String, Int>> =
    issues.groupingBy { it.repoFullName }
        .eachCount()
        .toList()
        .sortedByDescending { it.second }
        .take(topN)

@Composable
private fun BarSection(issues: List<Issue>) {
    val repoCounts = buildRepoCounts(issues)
    if (repoCounts.isEmpty()) {
        Text(
            "Not enough data to show this chart.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    DrafterTheme(dark = isSystemInDarkTheme()) {
        BarChart(
            renderer = BarChartRenderer(
                SimpleBarChartData(
                    labelsList = repoCounts.map { it.first },
                    values = repoCounts.map { it.second.toFloat() },
                    colors = repoCounts.indices.map { CategoricalPalette[it % CategoricalPalette.size] },
                ),
            ),
            modifier = Modifier.fillMaxWidth().height(280.dp),
            animate = true,
        )
    }
}

// ---------------------------------------------------------------------
// Scatter — comments vs. freshness
// ---------------------------------------------------------------------

@Composable
private fun ScatterSection(issues: List<Issue>, brandColor: Color) {
    DrafterTheme(dark = isSystemInDarkTheme()) {
        ScatterPlot(
            renderer = SimpleScatterPlotRenderer(
                ScatterPlotData(
                    points = issues.map {
                        Pair(
                            parseHoursAgo(it.updatedAtLabel),
                            it.commentCount.toFloat()
                        )
                    },
                    pointColors = issues.map { freshnessColor(it.updatedAtLabel, brandColor) },
                ),
            ),
            modifier = Modifier.fillMaxWidth().height(280.dp),
        )
    }
    Text(
        "x = hours since update (lower = fresher) · y = comment count",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}

// ---------------------------------------------------------------------
// Shared helpers (freshness legend, companion list, formatting)
// ---------------------------------------------------------------------

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

/**
 * Present regardless of which chart mode is active — even for the
 * aggregate views (Donut/Bar) where it's less tightly coupled to what's
 * on screen, always having a way to tap into a real issue is worth the
 * minor conceptual looseness, and keeps this simple rather than needing
 * per-mode companion content.
 */
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
