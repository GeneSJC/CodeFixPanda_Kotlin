package io.github.genesjc.codefixpanda.ui.toprepos

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.genesjc.codefixpanda.R
import io.github.genesjc.codefixpanda.model.RateLimitInfo
import io.github.genesjc.codefixpanda.ui.components.LabelInfoDialog
import io.github.genesjc.codefixpanda.ui.components.RateLimitDialog

/**
 * Shows the dialog named by [open], or nothing when it is null. The screen owns which one is open; each
 * dialog closes through [onDismiss]. Confirming the cache clear closes the dialog and then calls [onClearCacheConfirmed].
 */
@Composable
internal fun TopReposDialogs(
    open: TopReposDialog?,
    rateLimit: RateLimitInfo?,
    hasToken: Boolean,
    onOpenSettings: () -> Unit,
    onClearCacheConfirmed: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (open) {
        null -> Unit
        TopReposDialog.RANKING_INFO -> RankingInfoDialog(onDismiss = onDismiss)
        TopReposDialog.LABEL_INFO -> LabelInfoDialog(onDismiss = onDismiss)
        TopReposDialog.RATE_LIMIT -> RateLimitDialog(info = rateLimit, onDismiss = onDismiss)
        TopReposDialog.TOKEN_PROBLEM -> TokenProblemDialog(
            tokenRejected = hasToken,
            onOpenSettings = onOpenSettings,
            onDismiss = onDismiss,
        )
        TopReposDialog.CLEAR_CACHE -> ClearCacheDialog(
            onConfirm = {
                onDismiss()
                onClearCacheConfirmed()
            },
            onDismiss = onDismiss,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RankingInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_got_it)) }
        },
        title = { Text(stringResource(R.string.ranking_dialog_title)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.ranking_dialog_built_from_issues),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                Text(
                    stringResource(R.string.ranking_dialog_card_number),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                Text(
                    stringResource(R.string.ranking_dialog_future),
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
        title = { Text(stringResource(R.string.clear_cache_dialog_title)) },
        text = {
            Text(
                stringResource(R.string.clear_cache_dialog_message),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.action_clear_cache))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
