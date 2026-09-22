package com.example.githubissuefinder.ui.discover

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.githubissuefinder.model.IssueLabelFilter
import com.example.githubissuefinder.model.RepoIssueGroup
import com.example.githubissuefinder.ui.components.IssueCard
import com.example.githubissuefinder.ui.components.LabelInfoDialog
import com.example.githubissuefinder.ui.theme.colorScheme

/**
 * The repo-scoped issue list — see RepoIssuesViewModel's doc comment
 * for the full design reasoning (why no language filter, why issues
 * open in GitHub directly instead of the existing DetailScreen, why no
 * "no token" check here, and why this screen makes no network calls
 * at all as of this revision).
 *
 * v0.42 ("the data-layer swap"): takes a RepoIssueGroup instead of a
 * Repo. THE REPO DESCRIPTION SECTION IS GONE — it used to sit right
 * below the TopAppBar, truncated with a "Read more" dialog —
 * repo.description doesn't exist anymore because an issue-search
 * response never included it (RepoIssueGroup has no such field). A
 * real, visible reduction, not a layout tweak — flagged prominently
 * because it's exactly the kind of change worth someone noticing on
 * purpose rather than discovering by absence.
 *
 * v0.42, REVISED — the interactive label picker (LabelFilterRow,
 * disabledLabels, the "no open X issues for this repo" Snackbar) is
 * REMOVED, replaced with a small static pill showing the app's active
 * Issue Mode — non-interactive, just a confirmation of which mode
 * you're already in. This mirrors removing the mode dropdown from Top
 * Repos, for the same reason: Mode is a single, deliberate, whole-app
 * choice made once at the gate screen, and no screen at any depth
 * offers a second way to change it. The "ⓘ" info icon stays — it opens
 * the same shared LabelInfoDialog Feed uses, explaining what "good
 * first issue" / "help wanted" actually mean, which is still useful
 * context even with nothing left to pick between. Each IssueCard shows
 * an external-link badge (see IssueCard's doc comment) since tapping
 * one opens GitHub directly, not an in-app screen.
 *
 * v0.46 ("Check for More Issues"): the LazyColumn now always ends in a
 * footer reflecting uiState.isCheckingForMore / allIssuesLoaded /
 * checkForMoreError — an explicit "Check for More Issues" button (with
 * a small cost note underneath, "Uses one API call against your rate
 * limit"), a small inline spinner while that fetch is in flight, an
 * inline retry row on failure, or a plain "Showing all N issues" line
 * once the complete set has been fetched. See
 * RepoIssuesViewModel.checkForMoreIssues()'s doc comment for what this
 * closes (a real completeness gap — this screen previously had no way
 * to show more of a repo's issues than happened to be swept up while
 * Top Repos was built) and why it's deliberately manual rather than
 * automatic. The empty-state message ("No open issues to show.") moved
 * INSIDE the LazyColumn as its own item, rather than replacing it
 * entirely — the footer needs to render either way, so there can't
 * meaningfully be a screen with a list showing but no footer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoIssuesScreen(
    repoGroup: RepoIssueGroup,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RepoIssuesViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showInfoDialog by remember { mutableStateOf(false) }

    // Defensive fallback only — DiscoverNavHost guarantees a mode is
    // chosen before this screen is ever reachable. See IssueMode.kt.
    val activeMode = IssueMode.current ?: IssueLabelFilter.GOOD_FIRST_ISSUE

    LaunchedEffect(repoGroup.repoFullName) {
        viewModel.loadIfNeeded(repoGroup)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(repoGroup.repoDisplayTitle, style = MaterialTheme.typography.titleLarge)
                        Text(
                            repoGroup.repoFullName,
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
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
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
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "ⓘ",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { showInfoDialog = true },
                )
            }

            LazyColumn(
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
                                "No open issues to show.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
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
                item(key = "checkForMoreFooter") {
                    CheckForMoreIssuesFooter(
                        isCheckingForMore = uiState.isCheckingForMore,
                        allIssuesLoaded = uiState.allIssuesLoaded,
                        checkForMoreError = uiState.checkForMoreError,
                        currentIssueCount = uiState.issues.size,
                        onCheckForMore = { viewModel.checkForMoreIssues(repoGroup) },
                        onRetry = { viewModel.checkForMoreIssues(repoGroup) },
                    )
                }
            }
        }
    }

    if (showInfoDialog) {
        LabelInfoDialog(onDismiss = { showInfoDialog = false })
    }
}

/**
 * v0.46: the one place on this screen anything can be "loading" or
 * "errored" — see RepoIssuesUiState's own doc comment for why that's
 * scoped narrowly to this footer rather than the whole screen.
 * Deliberately a manual button, not automatic, with a visible cost
 * note — same "every network call stays a visible, intentional
 * choice" instinct Top Repos' own Load More button is built around.
 */
@Composable
private fun CheckForMoreIssuesFooter(
    isCheckingForMore: Boolean,
    allIssuesLoaded: Boolean,
    checkForMoreError: String?,
    currentIssueCount: Int,
    onCheckForMore: () -> Unit,
    onRetry: () -> Unit,
) {
    when {
        checkForMoreError != null -> Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                checkForMoreError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) {
                Text("Retry")
            }
        }
        isCheckingForMore -> Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp))
        }
        allIssuesLoaded -> Text(
            "Showing all $currentIssueCount issues.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        else -> Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = onCheckForMore, modifier = Modifier.fillMaxWidth()) {
                Text("Check for More Issues")
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
