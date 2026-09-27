package com.example.githubissuefinder.ui.discover

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.githubissuefinder.data.HiddenIssuesStore
import com.example.githubissuefinder.data.formatRelativeTime

/**
 * v0.51 ("Hide Issue", Part C of the Cache-Preserving Refresh sequence):
 * the permanent record of every issue hidden via the "✕" badge on
 * RepoIssuesScreen — reached from TopReposScreen's hamburger menu
 * ("Hidden") and from RepoIssuesScreen's own all-hidden empty state
 * ("View Hidden").
 *
 * Deliberately NOT built on IssueCard: HiddenIssue is a minimal
 * snapshot (id, title, repoFullName, htmlUrl, hiddenAtMs), not a full
 * Issue — no labels, comment count, or language to show, and that's by
 * design (see HiddenIssue's own doc comment for why the snapshot stays
 * small). A simple row is all there's real data for.
 *
 * The list comes directly from HiddenIssuesStore.all() — same
 * "no separate loading, no local copy" pattern RepoBookmarksScreen
 * already uses for RepoBookmarks.all(); un-hiding a row makes it
 * disappear immediately via Compose recomposition, nothing extra
 * wired here for that to happen.
 *
 * "Unhide all" sits in the top bar behind a confirmation dialog, per
 * the spec — the one destructive-feeling action on this screen (in the
 * sense of reversing a lot of history at once, even though every
 * individual unhide is itself already fully reversible by hiding
 * again).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenIssuesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val hiddenIssues = HiddenIssuesStore.all()
    var showUnhideAllConfirm by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Hidden") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    if (hiddenIssues.isNotEmpty()) {
                        TextButton(onClick = { showUnhideAllConfirm = true }) {
                            Text("Unhide all")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (hiddenIssues.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Nothing hidden yet — tap the ✕ on any issue in a repo's list to " +
                        "remove it from view. It stays reversible: come back here to unhide it any time.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(hiddenIssues, key = { it.id }) { hidden ->
                    Card(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(hidden.htmlUrl)))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(hidden.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                                Text(
                                    "${hidden.repoFullName} · Hidden ${formatRelativeTime(hidden.hiddenAtMs)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(onClick = { HiddenIssuesStore.unhide(context, hidden.id) }) {
                                Text("Unhide")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showUnhideAllConfirm) {
        AlertDialog(
            onDismissRequest = { showUnhideAllConfirm = false },
            title = { Text("Unhide all ${hiddenIssues.size} issues?") },
            text = { Text("They'll reappear wherever they're still cached — Top Repos and repo lists alike.") },
            confirmButton = {
                TextButton(onClick = {
                    HiddenIssuesStore.unhideAll(context)
                    showUnhideAllConfirm = false
                }) {
                    Text("Unhide all")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnhideAllConfirm = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}
