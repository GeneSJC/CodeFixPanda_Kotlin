package io.github.genesjc.codefixpanda.ui.toprepos

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import io.github.genesjc.codefixpanda.R
import io.github.genesjc.codefixpanda.ui.settings.ApiTokenHelpDialog

/**
 * Shown instead of RateLimitDialog on a token problem: none configured, or one GitHub
 * rejected (HTTP 401). Tapping the chip then is a setup problem, not "check back later",
 * so this explains the situation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TokenProblemDialog(
    tokenRejected: Boolean,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showHelp by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    onOpenSettings()
                },
            ) { Text(stringResource(R.string.action_open_settings)) }
        },
        dismissButton = {
            TextButton(onClick = { showHelp = true }) { Text(stringResource(R.string.token_problem_how_to)) }
        },
        title = { Text(stringResource(R.string.token_problem_title)) },
        text = {
            Text(
                stringResource(
                    if (tokenRejected) R.string.token_problem_rejected else R.string.token_problem_missing,
                ),
            )
        },
    )
    if (showHelp) {
        ApiTokenHelpDialog(onDismiss = { showHelp = false })
    }
}
