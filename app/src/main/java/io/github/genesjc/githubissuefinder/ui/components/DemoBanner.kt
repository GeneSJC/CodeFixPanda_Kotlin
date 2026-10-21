package io.github.genesjc.githubissuefinder.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.genesjc.githubissuefinder.R
import io.github.genesjc.githubissuefinder.ui.NEW_TOKEN_URL

/**
 * Tells the user that what they see is fake demo data, because no GitHub token is set. "Get a token"
 * opens GitHub's token page directly (signing in first if needed), and "Enter token" opens Settings to
 * paste it. Shown under the title bar of the screens that list issues.
 */
@Composable
fun DemoBanner(onEnterToken: () -> Unit, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 2.dp)) {
            Text(
                stringResource(R.string.demo_banner_message),
                style = MaterialTheme.typography.bodySmall,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { uriHandler.openUri(NEW_TOKEN_URL) }) {
                    Text(stringResource(R.string.action_get_token))
                }
                TextButton(onClick = onEnterToken) {
                    Text(stringResource(R.string.action_enter_token))
                }
            }
        }
    }
}
