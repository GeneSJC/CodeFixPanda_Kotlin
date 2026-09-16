package com.example.githubissuefinder.ui.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.githubissuefinder.data.CurrentFeedState
import com.example.githubissuefinder.data.RateLimitInfo
import com.example.githubissuefinder.data.RateLimitState
import com.example.githubissuefinder.ui.components.VersionLabel
import com.example.githubissuefinder.ui.insights.InsightsScreen
import io.androidpoet.drafter.gauge.GaugeChart
import io.androidpoet.drafter.gauge.GaugeChartRenderer
import io.androidpoet.drafter.gauge.model.GaugeData

// NOTE: GaugeChart, GaugeChartRenderer, and GaugeData are intentionally
// NOT imported here. Their constructor shape is confirmed directly from
// Drafter's official Kotlin README (not the unrelated Swift sibling
// project's changelog, which uses a different, incompatible API shape) —
// only the package path is unconfirmed, same situation as v0.7 and
// v0.15. See README.txt for the Android Studio quick-fix steps.

/**
 * Owns the single Feed destination's shared TopAppBar and a top TabRow
 * switching between "List" and "Insights" (see v0.11).
 *
 * The rate-limit indicator (see v0.12/v0.13) stays a compact tappable
 * chip in the TopAppBar — there genuinely isn't room to render a legible
 * dial in that constrained space. Tapping it opens a dialog that
 * includes an actual GaugeChart (see v0.17), not just plain text rows.
 *
 * selectedTab uses rememberSaveable (see v0.19) so returning from Detail
 * restores whichever tab (List/Insights) the person was actually on,
 * instead of always resetting to List.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedTabsScreen(
    bookmarkedIds: Set<Long>,
    onIssueClick: (issueId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    // rememberSaveable, not remember: Navigation Compose disposes this
    // destination's composition when Detail is pushed on top, and plain
    // remember does not survive that dispose/recompose cycle — it would
    // silently reset to List every time someone navigates back from
    // Detail, regardless of which tab they were actually on. showRateLimitDialog
    // stays plain remember deliberately: resetting a dialog to "closed"
    // on return from Detail is reasonable, expected behavior, not a bug.
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showRateLimitDialog by remember { mutableStateOf(false) }
    val feedState by CurrentFeedState.state.collectAsStateWithLifecycle()
    val rateLimit by RateLimitState.info.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text("Issue feed")
                            VersionLabel()
                        }
                    },
                    actions = {
                        rateLimit?.let { info ->
                            RateLimitChip(
                                info = info,
                                onClick = { showRateLimitDialog = true },
                            )
                        }
                    },
                )
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("List") },
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Insights (${feedState.issues.size})") },
                    )
                }
            }
        },
    ) { padding ->
        when (selectedTab) {
            0 -> FeedScreen(
                bookmarkedIds = bookmarkedIds,
                onIssueClick = onIssueClick,
                modifier = Modifier.padding(padding),
            )
            else -> InsightsScreen(
                onIssueClick = onIssueClick,
                modifier = Modifier.padding(padding),
            )
        }
    }

    if (showRateLimitDialog) {
        RateLimitDialog(
            info = rateLimit,
            onDismiss = { showRateLimitDialog = false },
        )
    }
}

@Composable
private fun RateLimitChip(info: RateLimitInfo, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.padding(end = 12.dp),
    ) {
        Text(
            "🔑 ${info.remaining}/${info.limit}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RateLimitDialog(info: RateLimitInfo?, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = { Text("Search API rate limit") },
        text = {
            if (info == null) {
                Text("No rate limit data available yet.")
            } else {
                Column {
                    GaugeChart(
                        renderer = GaugeChartRenderer(
                            GaugeData(
                                value = info.remaining.toFloat(),
                                min = 0f,
                                max = info.limit.toFloat().coerceAtLeast(1f),
                                label = "Remaining",
                                color = gaugeColorFor(info),
                            ),
                        ),
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                    )
                    RateLimitRow("Remaining", "${info.remaining}")
                    RateLimitRow("Limit", "${info.limit}")
                    RateLimitRow("Used", "${info.used}")
                    RateLimitRow("Resets in", info.resetsInLabel)
                    Text(
                        "This reflects GitHub's Search API limit specifically, which is " +
                            "separate from and stricter than the general REST API limit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
        },
    )
}

/**
 * Drafter's GaugeData takes a single color, not a built-in two-tone
 * green-to-red split — so instead of a fixed dial color, this computes
 * one dynamically from how much capacity remains: green when plentiful,
 * sliding toward red as it depletes. Same visual "danger" signal the
 * original speedometer concept was after, built on the confirmed, real
 * API rather than an unconfirmed multi-color feature.
 */
private fun gaugeColorFor(info: RateLimitInfo): Color {
    val ratio = (info.remaining.toFloat() / info.limit.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
    val depleted = Color(0xFFC62828)
    val full = Color(0xFF2E7D32)
    return lerp(depleted, full, ratio)
}

@Composable
private fun RateLimitRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
