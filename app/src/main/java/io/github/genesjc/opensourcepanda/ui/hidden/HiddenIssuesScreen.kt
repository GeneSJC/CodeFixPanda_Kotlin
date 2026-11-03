package io.github.genesjc.opensourcepanda.ui.hidden

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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.genesjc.opensourcepanda.R
import io.github.genesjc.opensourcepanda.ui.components.BrandBarIconButton
import io.github.genesjc.opensourcepanda.ui.components.BrandSoftBackground
import io.github.genesjc.opensourcepanda.ui.components.BrandTopAppBar
import io.github.genesjc.opensourcepanda.ui.text.relativeTimeText

/**
 * The record of every issue hidden with a card's hide badge, reached from the Top Repos menu and from
 * the repo screen's all-hidden state. A simple row, not IssueCard: HiddenIssue is a minimal snapshot
 * (id, title, repo, URL, hidden time), so there are no labels or comment count to show. The list
 * comes from HiddenIssuesViewModel, so un-hiding a row removes it immediately. "Unhide all" is in the
 * top bar behind a confirmation, since it reverses a lot of history at once.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenIssuesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HiddenIssuesViewModel = viewModel(),
) {
    val context = LocalContext.current
    val hiddenIssues by viewModel.hiddenIssues.collectAsStateWithLifecycle()
    var showUnhideAllConfirm by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // A change that could not be saved is reported here, as a message the user sees.
    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    BrandSoftBackground {
        Scaffold(
            containerColor = Color.Transparent,
            modifier = modifier,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                BrandTopAppBar(
                    title = { Text(stringResource(R.string.hidden_title)) },
                    navigationIcon = {
                        BrandBarIconButton(onClick = onBack) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = stringResource(R.string.action_back_description),
                            )
                        }
                    },
                    actions = {
                        if (hiddenIssues.isNotEmpty()) {
                            TextButton(
                                onClick = { showUnhideAllConfirm = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = LocalContentColor.current),
                            ) {
                                Text(stringResource(R.string.hidden_unhide_all))
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
                        stringResource(R.string.hidden_empty),
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
                                        stringResource(R.string.hidden_item_subtitle, hidden.repoFullName, relativeTimeText(hidden.hiddenAtMs)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                TextButton(onClick = { viewModel.unhide(hidden.id) }) {
                                    Text(stringResource(R.string.hidden_unhide))
                                }
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
            title = { Text(stringResource(R.string.hidden_unhide_all_title, hiddenIssues.size)) },
            text = { Text(stringResource(R.string.hidden_unhide_all_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.unhideAll()
                    showUnhideAllConfirm = false
                }) {
                    Text(stringResource(R.string.hidden_unhide_all))
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnhideAllConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}
