package io.github.genesjc.codefixpanda.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.genesjc.codefixpanda.R
import io.github.genesjc.codefixpanda.ui.welcome.NEW_TOKEN_URL

/** Opens GitHub's "new classic token" form with the description already filled in. */

/**
 * A step-by-step walkthrough for someone who has never made a GitHub personal access token,
 * reachable from Settings and from the "no token" and "token rejected" messages. It recommends a
 * classic token with every permission unchecked: the app only reads public data, so a leaked token
 * can do almost nothing. GitHub renames its menus now and then, so re-check the wording against its
 * current screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiTokenHelpDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.token_help_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    stringResource(R.string.token_help_intro),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Step("1.", stringResource(R.string.token_help_step1))
                Step(
                    "2.",
                    stringResource(R.string.token_help_step2),
                )
                Step(
                    "3.",
                    stringResource(R.string.token_help_step3),
                )
                Step(
                    "4.",
                    stringResource(R.string.token_help_step4),
                )
                Step("5.", stringResource(R.string.token_help_step5))
                Text(
                    stringResource(R.string.token_help_footer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { uriHandler.openUri(NEW_TOKEN_URL) }) { Text(stringResource(R.string.token_help_open_github)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
    )
}

@Composable
private fun Step(number: String, text: String) {
    Column(modifier = Modifier.padding(top = 10.dp)) {
        Text(
            "$number $text",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
