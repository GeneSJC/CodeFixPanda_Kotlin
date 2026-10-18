package com.example.githubissuefinder.ui.settings

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
import androidx.compose.ui.unit.dp

/** Opens GitHub's "new classic token" form with the description already filled in. */
private const val NEW_TOKEN_URL = "https://github.com/settings/tokens/new?description=GitHubIssueFinder"

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
        title = { Text("How to get a GitHub token") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "GitHubIssueFinder needs a free GitHub personal access token to search " +
                        "for issues. It only reads public information, so your token needs " +
                        "no permissions at all.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Step("1.", "Sign in at github.com. You can create a free account if you don't have one.")
                Step(
                    "2.",
                    "Open Settings → Developer settings → Personal access tokens → " +
                        "Tokens (classic), then choose Generate new token (classic). " +
                        "The button below takes you straight there.",
                )
                Step(
                    "3.",
                    "Name it \"GitHubIssueFinder\" and pick an expiration. Leave every " +
                        "permission (scope) unchecked.",
                )
                Step(
                    "4.",
                    "Tap Generate token, then copy the token. GitHub shows it only once, " +
                        "so copy it before leaving the page.",
                )
                Step("5.", "Come back here, paste it into the token box, and tap Save.")
                Text(
                    "Your token stays on this device and is sent only to GitHub. A token " +
                        "saved here replaces any token built into the app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { uriHandler.openUri(NEW_TOKEN_URL) }) { Text("Open GitHub") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
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
