package com.example.githubissuefinder.ui.feed

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.githubissuefinder.data.CurrentFeedState
import com.example.githubissuefinder.data.RateLimitInfo
import com.example.githubissuefinder.data.RateLimitState
import com.example.githubissuefinder.ui.components.VersionLabel
import com.example.githubissuefinder.ui.insights.InsightsScreen
import kotlin.math.cos
import kotlin.math.sin

/**
 * Owns the single Feed destination's shared TopAppBar and a top TabRow
 * switching between "List" and "Insights" (see v0.11).
 *
 * The rate-limit indicator (see v0.12/v0.13) stays a compact tappable
 * chip in the TopAppBar — there genuinely isn't room to render a legible
 * dial in that constrained space. Tapping it opens a dialog with a
 * speedometer-style dial.
 *
 * As of this Commit Milestone, that dial is a hand-built Compose Canvas
 * drawing (RateLimitDial below), NOT Drafter's GaugeChart. Reason:
 * Drafter's GaugeData exposes only a single color parameter — there's no
 * documented way to give it a fixed, position-based 4-stop gradient
 * (red-left to green-right, like a classic speedometer red-line). That's
 * a real, confirmed limit of the library's API, not something worth
 * guessing around — so this specific piece is drawn directly with
 * Canvas + Brush.sweepGradient, both stable, well-documented core Compose
 * APIs, rather than betting on an unconfirmed Drafter feature.
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
                    RateLimitDial(info = info)
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
 * Hand-drawn semicircular dial: a FIXED red(left)-to-green(right) track,
 * evenly stepped through orange and yellow — a classic speedometer
 * red-line convention, independent of the current value. A separate
 * pointer dot marks where the current remaining/limit ratio actually
 * falls along that fixed track. This is a deliberate design change from
 * v0.17: the track's colors no longer shift based on the value (that was
 * a workaround for Drafter's single-color limit) — now the track is
 * fixed and the pointer moves, which is both what was actually asked for
 * and a more standard gauge convention.
 *
 * Text ("29 Remaining") is rendered as ordinary Compose Text below the
 * Canvas, rather than drawn inside the arc via Canvas.drawText — simpler
 * and safer to lay out correctly than measuring/centering native text
 * inside a curved region.
 */
@Composable
private fun RateLimitDial(info: RateLimitInfo, modifier: Modifier = Modifier) {
    val ratio = (info.remaining.toFloat() / info.limit.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val dialogBackground = MaterialTheme.colorScheme.surface

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
            val strokeWidthPx = 20.dp.toPx()
            val radius = (minOf(size.width / 2f, size.height) - strokeWidthPx / 2f).coerceAtLeast(0f)
            val center = Offset(size.width / 2f, size.height - strokeWidthPx / 2f)
            val arcTopLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2f, radius * 2f)

            // Fixed track: red at 180° (left/"0" end) through orange and
            // yellow, to green at 360°/0° (right/"max" end) — the visible
            // half of a full sweepGradient. The 0.0-0.5 stops (the
            // invisible bottom-right-to-left half) are never drawn by the
            // drawArc call below, so their exact color doesn't matter.
            val trackBrush = Brush.sweepGradient(
                0.0f to Color(0xFF43A047),
                0.5f to Color(0xFFE53935),
                0.667f to Color(0xFFFB8C00),
                0.833f to Color(0xFFFDD835),
                1.0f to Color(0xFF43A047),
                center = center,
            )

            drawArc(
                brush = trackBrush,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
            )

            // Pointer position: ratio=0 (empty) -> 180° (left/red).
            // ratio=1 (full) -> 360°/0° (right/green).
            val pointerAngleRad = Math.toRadians((180f + 180f * ratio).toDouble())
            val pointerX = center.x + radius * cos(pointerAngleRad).toFloat()
            val pointerY = center.y + radius * sin(pointerAngleRad).toFloat()
            val pointerRadius = strokeWidthPx / 2f + 4.dp.toPx()

            drawCircle(color = dialogBackground, radius = pointerRadius, center = Offset(pointerX, pointerY))
            drawCircle(
                color = Color.Black.copy(alpha = 0.3f),
                radius = pointerRadius,
                center = Offset(pointerX, pointerY),
                style = Stroke(width = 2.dp.toPx()),
            )
        }

        Text("${info.remaining}", style = MaterialTheme.typography.headlineMedium)
        Text("Remaining", style = MaterialTheme.typography.bodySmall, color = onSurfaceVariant)

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("0", style = MaterialTheme.typography.labelSmall, color = onSurfaceVariant)
            Text("${info.limit}", style = MaterialTheme.typography.labelSmall, color = onSurfaceVariant)
        }
    }
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
