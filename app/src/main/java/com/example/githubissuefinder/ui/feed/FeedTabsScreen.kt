package com.example.githubissuefinder.ui.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.githubissuefinder.data.CurrentFeedState
import com.example.githubissuefinder.data.RateLimitInfo
import com.example.githubissuefinder.data.RateLimitState
import com.example.githubissuefinder.ui.insights.InsightsScreen

/**
 * Owns the single Feed destination's shared TopAppBar and a top TabRow
 * switching between "List" and "Insights" (see v0.11).
 *
 * The rate-limit indicator (see v0.12) is now a tappable chip rather than
 * plain text — given a bare Text with a hidden onClick has exactly the
 * same "you have to already know it's there" problem raised earlier about
 * pull-to-refresh gestures, it's styled with a tonal background so it
 * visibly reads as interactive at rest, not just on press. Tapping it
 * opens a small dialog with the full breakdown (remaining / limit / used /
 * resets in) rather than cramming all four numbers into the TopAppBar
 * itself.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedTabsScreen(
    bookmarkedIds: Set<Long>,
    onIssueClick: (issueId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showRateLimitDialog by remember { mutableStateOf(false) }
    val feedState by CurrentFeedState.state.collectAsStateWithLifecycle()
    val rateLimit by RateLimitState.info.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Issue feed") },
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
            else -> InsightsScreen(modifier = Modifier.padding(padding))
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
