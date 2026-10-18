package com.example.githubissuefinder.ui.discover

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.githubissuefinder.data.filterHiddenIssuesFlat
import com.example.githubissuefinder.data.formatRelativeTime
import kotlinx.coroutines.launch
import com.example.githubissuefinder.data.resolveActiveMode
import com.example.githubissuefinder.model.IssueLabelFilter
import com.example.githubissuefinder.model.LanguageFilter
import com.example.githubissuefinder.model.RepoIssueGroup
import com.example.githubissuefinder.ui.components.IssueCard
import com.example.githubissuefinder.ui.theme.colorScheme

/**
 * The issue list for one repo. It makes no network call on open: it shows the issues already
 * in the group and ends in a "Refresh this repo" button, an explicit call with a visible cost,
 * plus a spinner, retry row or result line. Hidden issues are filtered at display time from
 * the raw list; hiding shows an Undo Snackbar owned by this screen, because the card that
 * triggered it is about to leave the list. The README is an overlay over an always-composed
 * list, so it can animate its size freely. The pill in the filter row shows the active mode;
 * it is not a control, since the mode is one whole-app choice. More in docs/design-notes.md,
 * "Repo issues screen".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoIssuesScreen(
    repoGroup: RepoIssueGroup,
    language: LanguageFilter,
    onBack: () -> Unit,
    onOpenHidden: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RepoIssuesViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var readmeExpanded by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    // Hoisted so the list's scroll position is kept.
    val issueListState = rememberLazyListState()
    val hiddenIds by viewModel.hiddenIds.collectAsStateWithLifecycle()
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsStateWithLifecycle()
    val visibleIssues = filterHiddenIssuesFlat(uiState.issues, hiddenIds)

    // Defensive fallback only: DiscoverNavHost ensures a mode is chosen before this screen.
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val activeMode = resolveActiveMode(mode)

    LaunchedEffect(repoGroup.repoFullName) {
        viewModel.loadIfNeeded(repoGroup)
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(repoGroup.repoDisplayTitle, style = MaterialTheme.typography.titleLarge)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/${repoGroup.repoFullName}"),
                                ),
                            )
                        },
                    ) {
                        Text("↗", style = MaterialTheme.typography.titleLarge)
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .background(activeMode.colorScheme().primary, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            activeMode.displayName,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer,
                                RoundedCornerShape(8.dp),
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            "Kotlin",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    ReadmeToggleButton(
                        expanded = readmeExpanded,
                        onClick = { readmeExpanded = !readmeExpanded },
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

            // Always composed and full-size; the README overlay below is drawn on top of it.
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    state = issueListState,
                    modifier = Modifier.fillMaxSize(),
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
                                onClick = {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(issue.htmlUrl)),
                                    )
                                },
                                onToggleBookmark = { viewModel.toggleBookmark(issue) },
                                onHide = {
                                    viewModel.hideIssue(issue)
                                    coroutineScope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Issue hidden",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Short,
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.unhideIssue(issue.id)
                                        }
                                    }
                                },
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
                            onRefresh = { viewModel.refreshThisRepo(repoGroup, language) },
                            onRetry = { viewModel.refreshThisRepo(repoGroup, language) },
                        )
                    }
                }

                ReadmeExpandableBody(
                    expanded = readmeExpanded,
                    repoOwner = repoGroup.repoOwner,
                    repoName = repoGroup.repoName,
                    repoFullName = repoGroup.repoFullName,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp),
                )
            }
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
