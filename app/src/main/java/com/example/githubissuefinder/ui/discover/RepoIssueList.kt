package com.example.githubissuefinder.ui.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.githubissuefinder.data.formatRelativeTime
import com.example.githubissuefinder.model.Issue
import com.example.githubissuefinder.ui.components.IssueCard

/**
 * The repo screen's issue list: the cards, the two empty states (nothing matches, or everything
 * is hidden) and the refresh footer. It takes state and callbacks only. The scroll state is
 * passed in so the screen keeps the position, and the screen owns the hide Snackbar, so [onHide]
 * is a plain callback.
 */
@Composable
internal fun RepoIssueList(
    uiState: RepoIssuesUiState,
    visibleIssues: List<Issue>,
    bookmarkedIds: Set<Long>,
    listState: LazyListState,
    snackbarHostState: SnackbarHostState,
    onIssueClick: (Issue) -> Unit,
    onToggleBookmark: (Issue) -> Unit,
    onHide: (Issue) -> Unit,
    onOpenHidden: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (uiState.issues.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No open matching issues remain.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else if (visibleIssues.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "All matching issues in this repo are hidden.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    TextButton(onClick = onOpenHidden) {
                        Text("View Hidden")
                    }
                }
            }
        } else {
            items(visibleIssues, key = { it.id }) { issue ->
                IssueCard(
                    issue = issue,
                    isBookmarked = issue.id in bookmarkedIds,
                    showExternalLinkIndicator = true,
                    onClick = { onIssueClick(issue) },
                    onToggleBookmark = { onToggleBookmark(issue) },
                    onHide = { onHide(issue) },
                    snackbarHostState = snackbarHostState,
                )
            }
        }
        item(key = "refreshThisRepoFooter") {
            RefreshThisRepoFooter(
                isRefreshing = uiState.isRefreshing,
                hasRefreshed = uiState.hasRefreshed,
                lastRefreshedAtMs = uiState.lastRefreshedAtMs,
                refreshError = uiState.refreshError,
                currentIssueCount = uiState.issues.size,
                onRefresh = onRefresh,
                onRetry = onRefresh,
            )
        }
    }
}

/**
 * The one place on this screen that can be loading or errored. A manual button with a visible
 * cost note, in the same spirit as Load More on Top Repos. The idle state keeps the button and
 * adds a result line above it once a refresh has run.
 */
@Composable
private fun RefreshThisRepoFooter(
    isRefreshing: Boolean,
    hasRefreshed: Boolean,
    lastRefreshedAtMs: Long?,
    refreshError: String?,
    currentIssueCount: Int,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
) {
    when {
        refreshError != null -> Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                refreshError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) {
                Text("Retry")
            }
        }
        isRefreshing -> Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp))
        }
        else -> Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (hasRefreshed) {
                val statusText = if (currentIssueCount == 0) {
                    "No open issues remain"
                } else {
                    "All $currentIssueCount open issue${if (currentIssueCount == 1) "" else "s"}"
                }
                val checkedText = lastRefreshedAtMs?.let { " · checked ${formatRelativeTime(it)}" }.orEmpty()
                Text(
                    "$statusText$checkedText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                Text("Refresh this repo")
            }
            Text(
                "Uses one API call against your rate limit.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
