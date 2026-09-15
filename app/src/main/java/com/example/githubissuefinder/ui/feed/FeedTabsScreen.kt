package com.example.githubissuefinder.ui.feed

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.githubissuefinder.data.CurrentFeedState
import com.example.githubissuefinder.data.RateLimitState
import com.example.githubissuefinder.ui.insights.InsightsScreen

/**
 * Owns the single Feed destination's shared TopAppBar and a top TabRow
 * switching between "List" and "Insights" (see v0.11).
 *
 * Also now shows a rate-limit badge in the TopAppBar's actions area,
 * reading RateLimitState directly. This is deliberately NOT one of the
 * Insights tab's chart options (see the UI/UX roadmap PDF's corrected
 * item 1): GitHub's rate limit belongs to the credential making requests,
 * not to any particular search, so it's shown here — visible regardless
 * of which tab or filters are active — rather than bundled with
 * per-search visualizations. Shows nothing when null (no live search has
 * completed yet, or currently in sample-data mode).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedTabsScreen(
    bookmarkedIds: Set<Long>,
    onIssueClick: (issueId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
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
                            Text(
                                "${info.remaining}/${info.limit}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 16.dp),
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
}
