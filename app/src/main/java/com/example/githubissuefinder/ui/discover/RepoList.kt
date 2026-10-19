package com.example.githubissuefinder.ui.discover

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.githubissuefinder.R
import com.example.githubissuefinder.model.IssueLabelFilter
import com.example.githubissuefinder.model.LanguageFilter
import com.example.githubissuefinder.model.RepoIssueGroup
import kotlinx.coroutines.flow.StateFlow

/**
 * The list body: exactly one of no token, loading, error, empty, or the list with its
 * Load More footer. Shared by the portrait and landscape layouts.
 */
@Composable
internal fun RepoListBody(
    uiState: TopReposUiState,
    hiddenGroups: List<RepoIssueGroup>,
    snippets: StateFlow<Map<String, String?>>,
    currentMode: IssueLabelFilter,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onRetryLoadMore: () -> Unit,
    onRepoCardShown: (RepoIssueGroup) -> Unit,
    onRepoClick: (RepoIssueGroup, LanguageFilter) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Observed here, not in the screen, so only the list recomposes as README previews arrive.
    val snippetsByRepo by snippets.collectAsStateWithLifecycle()
    val rawGroupsByName = remember(uiState.repoGroups) { uiState.repoGroups.associateBy { it.repoFullName } }
    Box(modifier = modifier) {
        when {
            uiState.noTokenConfigured -> NoTokenMessage(onOpenSettings = onOpenSettings)
            uiState.isLoading && uiState.repoGroups.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            uiState.errorMessage != null && uiState.repoGroups.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(uiState.errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
                    Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
                        Text(stringResource(R.string.action_retry))
                    }
                }
            }
            hiddenGroups.isEmpty() -> CenteredMessage(
                text = stringResource(R.string.repo_list_empty, currentMode.displayName),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(hiddenGroups, key = { it.repoFullName }) { group ->
                    // The repo screen gets the raw group: it applies the hidden ids itself and needs every
                    // issue to show "all hidden" and to bring one back on Unhide.
                    RepoCard(
                        group = group,
                        snippet = snippetsByRepo[group.repoFullName],
                        onShown = { onRepoCardShown(group) },
                        onClick = { onRepoClick(rawGroupsByName[group.repoFullName] ?: group, uiState.selectedLanguage) },
                    )
                }
                item(key = "loadMoreFooter") {
                    LoadMoreFooter(
                        isLoadingMore = uiState.isLoadingMore,
                        canLoadMore = uiState.canLoadMore,
                        loadMoreError = uiState.loadMoreError,
                        onLoadMore = onLoadMore,
                        onRetry = onRetryLoadMore,
                    )
                }
            }
        }
    }
}

@Composable
private fun CenteredMessage(text: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * A repo card: title, match-count badge and a README preview (2 lines, expandable with a
 * chevron). The chevron is its own Surface(onClick) so tapping it does not open the repo.
 * The preview renders nothing while loading or when there is no README, since a spinner or a
 * "no README" note would be clutter. Fetched by ReadmeSnippetRepository, requested by the
 * ViewModel when the card first appears.
 */
@Composable
private fun RepoCard(
    group: RepoIssueGroup,
    snippet: String?,
    onShown: () -> Unit,
    onClick: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(300),
        label = "repoCardSnippetChevron",
    )
    LaunchedEffect(group.repoFullName) {
        onShown()
    }

    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    group.repoDisplayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        if (group.issues.isEmpty()) stringResource(R.string.repo_card_all_hidden) else matchCountLabel(group.matchCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
            if (!snippet.isNullOrBlank()) {
                if (expanded) {
                    // Expanded: the chevron gets its own row under the full snippet.
                    Column(modifier = Modifier.padding(top = 6.dp)) {
                        Text(
                            snippet,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        ReadmeSnippetChevron(
                            rotation = chevronRotation,
                            onClick = { expanded = !expanded },
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                } else {
                    // Collapsed: the chevron sits inline at the end of the 2-line text to save a row. Compose
                    // Text cannot place it after only the last wrapped line, so both lines narrow slightly.
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            snippet,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        ReadmeSnippetChevron(
                            rotation = chevronRotation,
                            onClick = { expanded = !expanded },
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun matchCountLabel(count: Int): String = pluralStringResource(R.plurals.match_count, count, count)

/**
 * The expand/collapse chevron for a card's README preview; used inline when collapsed and
 * on its own row when expanded.
 */
@Composable
private fun ReadmeSnippetChevron(rotation: Float, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        onClick = onClick,
        modifier = modifier,
    ) {
        Text(
            "⌄",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 1.dp)
                .rotate(rotation),
        )
    }
}


/**
 * The pagination footer: Load More, a spinner while loading, a retry row on failure, or an
 * end-of-results line. Load More is a manual tap, not auto-load on scroll, so each Search API
 * call is an explicit action.
 */
@Composable
private fun LoadMoreFooter(
    isLoadingMore: Boolean,
    canLoadMore: Boolean,
    loadMoreError: String?,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
) {
    when {
        loadMoreError != null -> Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                loadMoreError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) {
                Text(stringResource(R.string.action_retry))
            }
        }
        isLoadingMore -> Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp))
        }
        canLoadMore -> Button(onClick = onLoadMore, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_load_more))
        }
        else -> Text(
            stringResource(R.string.load_more_end),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
    }
}
