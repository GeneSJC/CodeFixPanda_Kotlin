package com.example.githubissuefinder.ui.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.githubissuefinder.data.SampleIssues
import com.example.githubissuefinder.ui.components.IssueCard

/**
 * Static feed screen: renders SampleIssues.all in a scrollable list.
 * No ViewModel, no state hoisting, no click handling beyond a no-op —
 * those come in the next iteration once we introduce MVVM. The goal here
 * is purely to validate the card layout against a realistic list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Issue feed") }) },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 12.dp,
                bottom = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(SampleIssues.all, key = { it.id }) { issue ->
                IssueCard(issue = issue, onClick = { /* wired up in a later iteration */ })
            }
        }
    }
}
