package io.github.genesjc.githubissuefinder.ui.discover

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.genesjc.githubissuefinder.R
import io.github.genesjc.githubissuefinder.ui.components.IssueCard

/**
 * The Bookmarks screen, reached from the Top Repos menu: every issue saved with the bookmark toggle.
 * It uses IssueCard with the external-link badge, like the repo screen, so tapping a card opens the
 * issue in GitHub. The list comes from BookmarksViewModel, so un-bookmarking here removes the card
 * immediately. It has its own SnackbarHost because IssueCard's bookmark toggle needs one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoBookmarksScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookmarksViewModel = viewModel(),
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val bookmarkedIssues by viewModel.bookmarks.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.bookmarks_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                },
            )
        },
    ) { padding ->
        if (bookmarkedIssues.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.bookmarks_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(bookmarkedIssues, key = { it.id }) { issue ->
                    IssueCard(
                        issue = issue,
                        isBookmarked = true, // guaranteed true — this screen only ever shows what's currently bookmarked
                        showExternalLinkIndicator = true,
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(issue.htmlUrl)),
                            )
                        },
                        onToggleBookmark = { viewModel.toggleBookmark(issue) },
                        snackbarHostState = snackbarHostState,
                    )
                }
            }
        }
    }
}
