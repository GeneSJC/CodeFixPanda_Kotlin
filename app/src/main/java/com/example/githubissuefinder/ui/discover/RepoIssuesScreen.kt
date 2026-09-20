package com.example.githubissuefinder.ui.discover

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.githubissuefinder.model.Repo
import com.example.githubissuefinder.ui.components.IssueCard
import com.example.githubissuefinder.ui.components.LabelFilterRow
import com.example.githubissuefinder.ui.components.LabelInfoDialog

/**
 * The repo-scoped issue list — see RepoIssuesViewModel's doc comment
 * for the full design reasoning (why no language filter, why issues
 * open in GitHub directly instead of the existing DetailScreen, why no
 * "no token" check here).
 *
 * The "ⓘ" beside the label filter row opens the shared LabelInfoDialog
 * (same one Feed uses, extracted in this same CM) explaining "good
 * first issue" / "help wanted." Each IssueCard shows an "↗" (see
 * IssueCard's doc comment for why not a chevron) since tapping one
 * opens GitHub directly, not an in-app screen.
 *
 * TopAppBar shows the repo's full name directly, which is deliberately
 * why this screen doesn't need a per-card repo-identity treatment the
 * way a mixed, cross-repo list would — the repo is already established
 * by the screen itself.
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

    LaunchedEffect(repo.id) {
        viewModel.loadIfNeeded(repo)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(repo.fullName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LabelFilterRow(
                    selected = uiState.selectedLabel,
                    onSelect = { label -> viewModel.onLabelSelected(repo, label) },
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
}
