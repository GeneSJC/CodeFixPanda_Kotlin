package com.example.githubissuefinder.ui.discover

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.githubissuefinder.ui.components.IssueCard

/**
 * v0.48: the repo-first flow's own Bookmarks screen — reached from
 * TopReposScreen's hamburger menu ("Bookmarks"), showing every issue
 * currently saved via the bookmark toggle IssueCard now offers (see
 * that file's own doc comment). Deliberately its own screen, not a
 * tab or a section grafted onto an existing one — a genuinely separate
 * place to come back to.
 *
 * "Same buttons and layout as from the repo view," per how this was
 * asked for: every card here uses IssueCard with
 * showExternalLinkIndicator = true, exactly like RepoIssuesScreen's
 * own cards — tapping one opens the issue directly in GitHub, the same
 * Intent.ACTION_VIEW pattern, not some third in-app behavior invented
 * just for this screen.
 *
 * The displayed list comes from BookmarksViewModel (which observes
 * BookmarksRepository) — no separate loading, no local copy kept in
 * sync by hand. Un-bookmarking a card FROM THIS SCREEN makes it
 * disappear from the list immediately — falls out naturally from the
 * ViewModel's list being a StateFlow this screen collects, nothing
 * special written here to make that happen.
 *
 * Has its own SnackbarHost — IssueCard's bookmark toggle needs one to
 * show its "Bookmarked"/"Unbookmarked" message (see IssueCard's own
 * doc comment for why that action specifically uses a real Snackbar
 * rather than the Toast pattern Copy Link uses).
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
                title = { Text("Bookmarks") },
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
                    "No bookmarks yet — tap the ★ on any issue to save it here.",
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
