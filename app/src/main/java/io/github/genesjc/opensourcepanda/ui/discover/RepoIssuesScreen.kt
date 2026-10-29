package io.github.genesjc.opensourcepanda.ui.discover

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.genesjc.opensourcepanda.R
import io.github.genesjc.opensourcepanda.data.filterHiddenIssuesFlat
import io.github.genesjc.opensourcepanda.data.resolveActiveMode
import io.github.genesjc.opensourcepanda.model.IssueLabelFilter
import io.github.genesjc.opensourcepanda.model.LanguageFilter
import io.github.genesjc.opensourcepanda.model.RepoIssueGroup
import io.github.genesjc.opensourcepanda.ui.ads.AdBannerSlot
import io.github.genesjc.opensourcepanda.ui.ads.AdFreeOfferLine
import io.github.genesjc.opensourcepanda.ui.components.ModePill
import io.github.genesjc.opensourcepanda.ui.findActivity
import io.github.genesjc.opensourcepanda.ui.components.BrandBarIconButton
import io.github.genesjc.opensourcepanda.ui.components.BrandSoftBackground
import io.github.genesjc.opensourcepanda.ui.components.BrandTopAppBar
import io.github.genesjc.opensourcepanda.ui.nameRes
import kotlinx.coroutines.launch

/**
 * The issue list for one repo. It makes no network call on open: it shows the issues already
 * in the group and ends in a "Refresh this repo" button, an explicit call with a visible cost,
 * plus a spinner, retry row or result line. Hidden issues are filtered at display time from
 * the raw list; hiding shows an Undo Snackbar owned by this screen, because the card that
 * triggered it is about to leave the list. The README is an overlay over an always-composed
 * list, so it can animate its size freely. The pill in the filter row shows the active mode;
 * it is not a control, since the mode is one whole-app choice. More in docs/design-notes.md,
 * "Repo issues screen".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoIssuesScreen(
    repoGroup: RepoIssueGroup,
    language: LanguageFilter,
    onBack: () -> Unit,
    onOpenHidden: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RepoIssuesViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDemo by viewModel.isDemo.collectAsStateWithLifecycle()
    val canShowAds by viewModel.canShowAds.collectAsStateWithLifecycle()
    val adFreeUntilMs by viewModel.adFreeUntilMs.collectAsStateWithLifecycle()
    val canOfferAdFree by viewModel.canOfferAdFree.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var readmeExpanded by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    // Hoisted so the list's scroll position is kept.
    val issueListState = rememberLazyListState()
    val hiddenIds by viewModel.hiddenIds.collectAsStateWithLifecycle()
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsStateWithLifecycle()
    val issueHiddenMessage = stringResource(R.string.issue_hidden_message)
    val undoLabel = stringResource(R.string.action_undo)
    val visibleIssues = filterHiddenIssuesFlat(uiState.issues, hiddenIds)

    // Defensive fallback only: DiscoverNavHost ensures a mode is chosen before this screen.
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val activeMode = resolveActiveMode(mode)

    LaunchedEffect(repoGroup.repoFullName) {
        viewModel.loadIfNeeded(repoGroup)
    }

    BrandSoftBackground {
        Scaffold(
            containerColor = Color.Transparent,
            modifier = modifier,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = { AdBannerSlot(visible = canShowAds, modifier = Modifier.navigationBarsPadding()) },
            topBar = {
                BrandTopAppBar(
                    title = {
                        Column {
                            Text(repoGroup.repoDisplayTitle, style = MaterialTheme.typography.titleLarge)
                            AdFreeOfferLine(
                                adFreeUntilMs = adFreeUntilMs,
                                canOffer = canOfferAdFree,
                                onOffer = { context.findActivity()?.let(viewModel::onAdFreeOffer) },
                            )
                        }
                    },
                    navigationIcon = {
                        BrandBarIconButton(onClick = onBack) {
                            Text("←", style = MaterialTheme.typography.titleLarge)
                        }
                    },
                    actions = {
                        if (isDemo) DemoChip(modifier = Modifier.padding(end = 8.dp))
                        BrandBarIconButton(
                            onClick = {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://github.com/${repoGroup.repoFullName}"),
                                    ),
                                )
                            },
                        ) {
                            Text("↗\uFE0E", style = MaterialTheme.typography.titleLarge)
                        }
                    },
                )
            },
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                RepoIssuesFilterRow(
                    mode = activeMode,
                    language = language,
                    readmeExpanded = readmeExpanded,
                    onToggleReadme = { readmeExpanded = !readmeExpanded },
                )

                // Always composed and full-size; the README overlay below is drawn on top of it.
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    RepoIssueList(
                        uiState = uiState,
                        visibleIssues = visibleIssues,
                        bookmarkedIds = bookmarkedIds,
                        listState = issueListState,
                        snackbarHostState = snackbarHostState,
                        onIssueClick = { issue ->
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(issue.htmlUrl)))
                        },
                        onToggleBookmark = { issue -> viewModel.toggleBookmark(issue) },
                        onHide = { issue ->
                            viewModel.hideIssue(issue)
                            coroutineScope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = issueHiddenMessage,
                                    actionLabel = undoLabel,
                                    duration = SnackbarDuration.Short,
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.unhideIssue(issue.id)
                                }
                            }
                        },
                        onOpenHidden = onOpenHidden,
                        onRefresh = { viewModel.refreshThisRepo(repoGroup, language) },
                        modifier = Modifier.fillMaxSize(),
                    )

                    ReadmeExpandableBody(
                        expanded = readmeExpanded,
                        repoOwner = repoGroup.repoOwner,
                        repoName = repoGroup.repoName,
                        repoFullName = repoGroup.repoFullName,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}

/** The row under the title: the active mode, the language, and the README toggle. */
@Composable
private fun RepoIssuesFilterRow(
    mode: IssueLabelFilter,
    language: LanguageFilter,
    readmeExpanded: Boolean,
    onToggleReadme: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The mode's name takes the room the two chips leave and wraps; the chips stay on one line.
            ModePill(mode = mode, modifier = Modifier.weight(1f, fill = false))
            Surface(
                modifier = Modifier.padding(start = 8.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Text(
                    stringResource(language.nameRes()),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
            ReadmeToggleButton(
                expanded = readmeExpanded,
                onClick = onToggleReadme,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
