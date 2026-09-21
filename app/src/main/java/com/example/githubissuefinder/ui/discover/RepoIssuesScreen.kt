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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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

            if (uiState.issues.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No open issues to show.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
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
}
