package com.example.githubissuefinder.ui.feed

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.githubissuefinder.data.CurrentFeedState
import com.example.githubissuefinder.ui.insights.InsightsScreen

/**
 * Owns the single Feed destination's shared TopAppBar and a top TabRow
 * switching between "List" (the issue feed + filters) and "Insights" (the
 * treemap view of the same current results).
 *
 * Insights was previously its own top-level bottom-nav destination —
 * merged here because it was never conceptually a separate place, just a
 * different lens on Feed's current data (it has always sourced from
 * CurrentFeedState, which Feed populates). Bottom nav is now 2 items
 * (Feed, Bookmarks) instead of 3.
 *
 * The result-count badge on the Insights tab label reads directly from
 * CurrentFeedState — independent of FeedScreen's own FeedViewModel scope,
 * since CurrentFeedState is a plain singleton, not view-model-scoped.
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

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                TopAppBar(title = { Text("Issue feed") })
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
