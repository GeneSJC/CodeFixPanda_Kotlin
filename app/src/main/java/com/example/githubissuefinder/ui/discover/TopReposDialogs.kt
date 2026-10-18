package com.example.githubissuefinder.ui.discover

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RankingInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Got it") }
        },
        title = { Text("How are these repos ranked?") },
        text = {
            Column {
                Text(
                    "This list is built directly from matching issues, not from a separate " +
                        "repo search — every repo shown is guaranteed to have at least one " +
                        "open issue for whichever mode you're in.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                Text(
                    "The number on each card is simply how many matching issues that repo " +
                        "had in this batch — repos with more matches sort higher. It isn't a " +
                        "measure of the repo's overall popularity or health (stars, forks, " +
                        "how recently it was updated) the way an earlier version of this " +
                        "screen used — that data isn't available without extra lookups this " +
                        "version deliberately doesn't make yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                Text(
                    "A future update may add that context back for repos worth a closer look, " +
                        "without giving up the guarantee that every repo shown here actually " +
                        "has something open for you.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}


/** Confirms Clear Cache, which wipes every cached list; bookmarks and hidden issues are kept. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ClearCacheDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clear cache?") },
        text = {
            Text(
                "Removes every cached repo/issue list so the next load fetches live data. " +
                    "Bookmarks and hidden issues are kept.",
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Clear Cache")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
