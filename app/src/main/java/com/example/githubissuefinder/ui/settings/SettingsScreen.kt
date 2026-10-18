package com.example.githubissuefinder.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.githubissuefinder.data.TokenSource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Where a user supplies their own GitHub personal access token — needed
 * because the build-time token (local.properties) only exists for
 * someone building from source, so a person who installs a built APK
 * would otherwise have no way to make the app work at all.
 *
 * The token is shown in the clear as it is typed, on purpose: personal
 * access tokens are long strings of near-identical characters, and
 * being able to see what was pasted makes mistakes much easier to
 * spot. It needs no permissions and is stored only on this device (see
 * ApiTokenRepository for why that is an acceptable tradeoff).
 *
 * The text in the field is local draft state, not ViewModel state: it
 * only matters until Save is tapped. It is keyed on the saved token so
 * it resets to the saved value whenever that changes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ApiTokenViewModel = koinViewModel(),
) {
    val savedToken by viewModel.userToken.collectAsStateWithLifecycle()
    val source by viewModel.tokenSource.collectAsStateWithLifecycle()
    var draft by rememberSaveable(savedToken) { mutableStateOf(savedToken) }
    var showHelp by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("GitHub token", style = MaterialTheme.typography.titleMedium)
            Text(
                "GitHubIssueFinder needs a GitHub personal access token to search for " +
                    "issues. It needs no permissions, and it stays on this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                sourceDescription(source),
                style = MaterialTheme.typography.bodyMedium,
                color = if (source == TokenSource.NONE) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                label = { Text("Your GitHub token") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Ascii,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.saveToken(draft) },
                    enabled = draft.trim() != savedToken,
                ) {
                    Text("Save")
                }
                if (savedToken.isNotEmpty()) {
                    OutlinedButton(onClick = { viewModel.clearToken() }) {
                        Text("Remove my token", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            TextButton(onClick = { showHelp = true }) {
                Text("How do I get a token?")
            }
        }
    }

    if (showHelp) {
        ApiTokenHelpDialog(onDismiss = { showHelp = false })
    }
}

private fun sourceDescription(source: TokenSource): String = when (source) {
    TokenSource.USER -> "Using the token you entered."
    TokenSource.BUILD -> "Using the token built into this app. Enter your own below to replace it."
    TokenSource.NONE -> "No token yet — the app can't search GitHub until you add one."
}
