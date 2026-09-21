package com.example.githubissuefinder.ui.discover

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.githubissuefinder.model.IssueLabelFilter
import com.example.githubissuefinder.model.Repo
import com.example.githubissuefinder.ui.components.IssueCard
import com.example.githubissuefinder.ui.components.LabelFilterRow
import com.example.githubissuefinder.ui.components.LabelInfoDialog
import kotlinx.coroutines.launch

private const val DESCRIPTION_TRUNCATE_LENGTH = 200

/**
 * The repo-scoped issue list — see RepoIssuesViewModel's doc comment
 * for the full design reasoning (why no language filter, why issues
 * open in GitHub directly instead of the existing DetailScreen, why no
 * "no token" check here).
 *
 * The "ⓘ" beside the label filter row opens the shared LabelInfoDialog
 * (same one Feed uses, extracted in v0.38) explaining "good first
 * issue" / "help wanted." Each IssueCard shows an external-link badge
 * (see IssueCard's doc comment) since tapping one opens GitHub
 * directly, not an in-app screen.
 *
 * v0.39 ("CM B"): TopAppBar now shows repo.displayTitle (humanized,
 * readable — see RepoNameFormatting.kt) as the primary title, with
 * repo.fullName (the raw owner/name path) as a smaller line
 * underneath, matching the same two-tier treatment applied to
 * TopReposScreen's cards and IssueCard's repo identity row.
 *
 * The repo's description sits just below the TopAppBar (not inside the
 * TopAppBar composable itself — Material3's TopAppBar is designed to
 * stay compact, and cramming title + path + a full description into it
 * would fight that). Truncated to DESCRIPTION_TRUNCATE_LENGTH characters
 * with a "Read more" link opening a simple dialog with the full text —
 * chosen over an inline expand-in-place for reliability (no layout
 * height animation to get right blind); easy to swap for an inline
 * expand later if a dialog feels heavier than wanted once seen.
 *
 * v0.40 ("CM C"): the label filter row now grays out whichever option
 * has zero matching issues for this repo. The repo's own pre-fetched
 * mode uses the count already fetched back on Top Repos
 * (repo.issueCount); the other label has no such pre-fetched count, so
 * it's learned live — see RepoIssuesViewModel's doc comment. Tapping a
 * grayed-out option shows a Snackbar explaining why, rather than
 * reloading nothing.
 *
 * v0.41 ("Issue Mode" shell) fix: this file was NOT in v0.41's own
 * file list, but v0.41 renamed Repo.goodFirstIssueCount to issueCount
 * — left unfixed, this wouldn't compile. Also fixed here, not just the
 * rename: which label repo.issueCount == 0 should disable is now
 * IssueMode.current (whichever mode was active when Top Repos fetched
 * it), not a hardcoded GOOD_FIRST_ISSUE — same fix, same reasoning, as
 * the one made in RepoIssuesViewModel.kt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoIssuesScreen(
    repo: Repo,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RepoIssuesViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showInfoDialog by remember { mutableStateOf(false) }
    var showFullDescription by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // The label repo.issueCount was actually fetched for — see the
    // doc comment above. Defensive fallback only; DiscoverNavHost
    // guarantees a mode is chosen before this screen is reachable.
    val fetchedForMode = IssueMode.current ?: IssueLabelFilter.GOOD_FIRST_ISSUE
    val disabledLabels = remember(repo.issueCount, uiState.otherLabelKnownEmpty, fetchedForMode) {
        buildSet {
            if (repo.issueCount == 0) add(fetchedForMode)
            if (uiState.otherLabelKnownEmpty) {
                add(IssueLabelFilter.entries.first { it != fetchedForMode })
            }
        }
    }

    LaunchedEffect(repo.id) {
        viewModel.loadIfNeeded(repo)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(repo.displayTitle, style = MaterialTheme.typography.titleLarge)
                        Text(
                            repo.fullName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (repo.description.isNotBlank()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    val isTruncated = repo.description.length > DESCRIPTION_TRUNCATE_LENGTH
                    val shownText = if (isTruncated) {
                        repo.description.take(DESCRIPTION_TRUNCATE_LENGTH).trimEnd() + "…"
                    } else {
                        repo.description
                    }
                    Text(
                        shownText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (isTruncated) {
                        Text(
                            "Read more",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { showFullDescription = true }
                                .padding(top = 2.dp),
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                LabelFilterRow(
                    selected = uiState.selectedLabel,
                    onSelect = { label -> viewModel.onLabelSelected(repo, label) },
                    disabledOptions = disabledLabels,
                    onDisabledOptionTapped = { label ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                "No open ${label.displayName} issues for this repo",
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "ⓘ",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { showInfoDialog = true }
                        .padding(end = 16.dp),
                )
            }

            when {
                uiState.isLoading && uiState.issues.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
                uiState.errorMessage != null && uiState.issues.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
                        Button(
                            onClick = { viewModel.retry(repo) },
                            modifier = Modifier.padding(top = 12.dp),
                        ) {
                            Text("Retry")
                        }
                    }
                }
                uiState.issues.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No open issues with this label right now.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(uiState.issues, key = { it.id }) { issue ->
                        IssueCard(
                            issue = issue,
                            isBookmarked = false,
                            showExternalLinkIndicator = true,
                            onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(issue.htmlUrl)),
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    if (showInfoDialog) {
        LabelInfoDialog(onDismiss = { showInfoDialog = false })
    }

    if (showFullDescription) {
        AlertDialog(
            onDismissRequest = { showFullDescription = false },
            confirmButton = {
                TextButton(onClick = { showFullDescription = false }) { Text("Close") }
            },
            title = { Text(repo.displayTitle) },
            text = { Text(repo.description) },
        )
    }
}
