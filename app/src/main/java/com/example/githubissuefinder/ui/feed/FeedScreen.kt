package com.example.githubissuefinder.ui.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.githubissuefinder.model.IssueLabelFilter
import com.example.githubissuefinder.model.LanguageFilter
import com.example.githubissuefinder.ui.components.IssueCard

/**
 * The "List" tab's content: issue list, filter chips, loading/error/empty
 * states, and the sample-data banner. No longer owns its own Scaffold or
 * TopAppBar — as of this Commit Milestone it's hosted inside
 * FeedTabsScreen, which owns the shared TopAppBar and the List/Insights
 * TabRow above it. Bookmark state (bookmarkedIds, onIssueClick) is still
 * owned by AppNavHost and passed down, unchanged from v0.3.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    bookmarkedIds: Set<Long>,
    onIssueClick: (issueId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FeedViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        if (uiState.isSampleData) {
            SampleDataBanner()
        }

        LabelFilterRow(
            selected = uiState.selectedLabel,
            onSelect = viewModel::onLabelSelected,
        )
        LanguageFilterRow(
            selected = uiState.selectedLanguage,
            onSelect = viewModel::onLanguageSelected,
        )

        when {
            uiState.isLoading && uiState.issues.isEmpty() -> LoadingState()
            uiState.errorMessage != null && uiState.issues.isEmpty() -> ErrorState(
                message = uiState.errorMessage.orEmpty(),
                onRetry = viewModel::retry,
            )
            uiState.issues.isEmpty() -> EmptyState()
            else -> IssueList(
                issues = uiState.issues,
                bookmarkedIds = bookmarkedIds,
                onIssueClick = onIssueClick,
            )
        }
    }
}

@Composable
private fun SampleDataBanner() {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(
            "Showing sample data — add a GitHub token to local.properties to see live issues.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.tertiary,
        )
    }
}

@Composable
private fun LabelFilterRow(selected: IssueLabelFilter, onSelect: (IssueLabelFilter) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
    ) {
        items(IssueLabelFilter.entries) { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(option.displayName) },
            )
        }
    }
}

@Composable
private fun LanguageFilterRow(selected: LanguageFilter, onSelect: (LanguageFilter) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
    ) {
        items(LanguageFilter.entries) { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(option.displayName) },
            )
        }
    }
}

@Composable
private fun IssueList(
    issues: List<com.example.githubissuefinder.model.Issue>,
    bookmarkedIds: Set<Long>,
    onIssueClick: (Long) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(issues, key = { it.id }) { issue ->
            IssueCard(
                issue = issue,
                isBookmarked = issue.id in bookmarkedIds,
                onClick = { onIssueClick(issue.id) },
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Text(
                "Loading issues…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            "No open issues matched these filters.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = MaterialTheme.colorScheme.error)
            Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
                Text("Retry")
            }
        }
    }
}
