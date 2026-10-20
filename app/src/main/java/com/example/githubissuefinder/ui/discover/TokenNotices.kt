package com.example.githubissuefinder.ui.discover

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.githubissuefinder.R
import com.example.githubissuefinder.ui.settings.ApiTokenHelpDialog

/**
 * The "no token" state: an explanation and a button to Settings. Saving a token reloads
 * the list on its own.
 */
@Composable
internal fun NoTokenMessage(onOpenSettings: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.no_token_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onOpenSettings, modifier = Modifier.padding(top = 12.dp)) {
                Text(stringResource(R.string.no_token_add))
            }
        }
    }
}

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
