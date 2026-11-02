package io.github.genesjc.opensourcepanda.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.genesjc.opensourcepanda.R
import io.github.genesjc.opensourcepanda.model.TokenSource
import io.github.genesjc.opensourcepanda.ui.components.IndependentNotice
import io.github.genesjc.opensourcepanda.ui.components.TokenField
import io.github.genesjc.opensourcepanda.ui.components.BrandBarIconButton
import io.github.genesjc.opensourcepanda.ui.components.BrandSoftBackground
import io.github.genesjc.opensourcepanda.ui.components.BrandTopAppBar

/**
 * Where a user supplies their own GitHub token; the build-time token exists only for someone
 * building from source. The token is shown in the clear as it is typed, on purpose: near-identical
 * characters make mistakes hard to spot otherwise, and it needs no permissions and stays on the
 * device (see ApiTokenRepository). The text in the field is local draft state, keyed on the saved
 * token so it resets when that changes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ApiTokenViewModel = viewModel(),
) {
    val savedToken by viewModel.userToken.collectAsStateWithLifecycle()
    val currentToken by viewModel.currentToken.collectAsStateWithLifecycle()
    val source by viewModel.tokenSource.collectAsStateWithLifecycle()
    // The field starts with the token in use (masked), whether the user saved it or the build supplied it.
    var draft by rememberSaveable(currentToken) { mutableStateOf(currentToken) }
    var showHelp by rememberSaveable { mutableStateOf(false) }

    BrandSoftBackground {
        Scaffold(
            containerColor = Color.Transparent,
            modifier = modifier,
            topBar = {
                BrandTopAppBar(
                    title = { Text(stringResource(R.string.settings_title)) },
                    navigationIcon = {
                        BrandBarIconButton(onClick = onBack) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = stringResource(R.string.action_back_description),
                            )
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
                Text(stringResource(R.string.settings_token_heading), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.settings_token_explainer),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(sourceDescriptionRes(source)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (source == TokenSource.NONE) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
                TokenField(value = draft, onValueChange = { draft = it }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.saveToken(draft) },
                        enabled = draft.trim() != currentToken,
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                    // Always shown, greyed out while there is no saved token to remove.
                    OutlinedButton(onClick = { viewModel.clearToken() }, enabled = savedToken.isNotEmpty()) {
                        Text(stringResource(R.string.settings_remove_token), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (source == TokenSource.BUILD) {
                    Text(
                        stringResource(R.string.token_remove_unavailable_build),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { showHelp = true }) {
                    Text(stringResource(R.string.settings_how_to_get_token))
                }
                IndependentNotice(textAlign = TextAlign.Start)
            }
        }
    }

    if (showHelp) {
        ApiTokenHelpDialog(onDismiss = { showHelp = false })
    }
}

@StringRes
private fun sourceDescriptionRes(source: TokenSource): Int = when (source) {
    TokenSource.USER -> R.string.token_source_user
    TokenSource.BUILD -> R.string.token_source_build
    TokenSource.NONE -> R.string.token_source_none
}
