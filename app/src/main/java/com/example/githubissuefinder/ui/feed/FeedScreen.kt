package com.example.githubissuefinder.ui.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.githubissuefinder.ui.components.IssueCard
import com.example.githubissuefinder.ui.components.LabelFilterRow
import com.example.githubissuefinder.ui.components.LabelInfoDialog
import com.example.githubissuefinder.ui.components.LanguageFilterRow

/**
 * Now backed by FeedViewModel and real GitHub Search API data, instead of
 * SampleIssues.kt directly. Bookmark state (bookmarkedIds, onIssueClick)
 * is still owned by AppNavHost and passed down, unchanged from v0.3.
 *
 * Shows a "Showing N of M open matches" caption above the list when M
 * exceeds N — GitHub's Search API caps each response at per_page=30, so
 * without this, two very differently-sized searches could look identical
 * just because both hit the same page ceiling.
 *
 * The label filter row now has a "ⓘ" beside it (same pattern as
 * Insights' per-chart info dialog from v0.20), explaining what
 * "good first issue" and "help wanted" actually mean — these are
 * GitHub-wide conventions applied by each repo's own maintainers, not
 * something this app defines or enforces.
 *
 * LanguageFilterRow is now shared with the Discover "Top Repos" screen
 * (v0.26) — extracted to ui/components/ so both render identical options
 * from one source. LabelFilterRow and LabelInfoDialog (both v0.38) got
 * the same treatment, shared with the new repo-scoped RepoIssuesScreen.
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
    var showLabelInfoDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        if (uiState.isSampleData) {
            SampleDataBanner()
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            LabelFilterRow(
                selected = uiState.selectedLabel,
                onSelect = viewModel::onLabelSelected,
                modifier = Modifier.weight(1f),
            )
            Text(
                "ⓘ",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { showLabelInfoDialog = true }
                    .padding(end = 16.dp),
            )
        }
        LanguageFilterRow(
            selected = uiState.selectedLanguage,
            onSelect = viewModel::onLanguageSelected,
        )

        if (uiState.issues.isNotEmpty()) {
            ResultCountCaption(shown = uiState.issues.size, total = uiState.totalResultCount)
        }

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

    if (showLabelInfoDialog) {
        LabelInfoDialog(onDismiss = { showLabelInfoDialog = false })
    }
}

@Composable
private fun ResultCountCaption(shown: Int, total: Int) {
    val text = if (total > shown) {
        "Showing $shown of $total open matches"
    } else {
        "Showing $shown open matches"
    }
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 4.dp, end = 16.dp),
    )
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
