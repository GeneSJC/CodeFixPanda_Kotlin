package com.example.githubissuefinder.ui.discover

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.githubissuefinder.data.HiddenIssuesStore
import com.example.githubissuefinder.data.RepoBookmarks
import com.example.githubissuefinder.data.filterHiddenIssuesFlat
import com.example.githubissuefinder.data.formatRelativeTime
import kotlinx.coroutines.launch
import com.example.githubissuefinder.data.resolveActiveMode
import com.example.githubissuefinder.model.IssueLabelFilter
import com.example.githubissuefinder.model.LanguageFilter
import com.example.githubissuefinder.model.RepoIssueGroup
import com.example.githubissuefinder.ui.components.IssueCard
import com.example.githubissuefinder.ui.theme.colorScheme

/**
 * The repo-scoped issue list — see RepoIssuesViewModel's doc comment
 * for the full design reasoning (why no language filter, why issues
 * open in GitHub directly instead of the existing DetailScreen, why no
 * "no token" check here, and why this screen makes no network calls
 * at all as of this revision).
 *
 * v0.42 ("the data-layer swap"): takes a RepoIssueGroup instead of a
 * Repo. THE REPO DESCRIPTION SECTION IS GONE — it used to sit right
 * below the TopAppBar, truncated with a "Read more" dialog —
 * repo.description doesn't exist anymore because an issue-search
 * response never included it (RepoIssueGroup has no such field). A
 * real, visible reduction, not a layout tweak — flagged prominently
 * because it's exactly the kind of change worth someone noticing on
 * purpose rather than discovering by absence.
 *
 * v0.42, REVISED — the interactive label picker (LabelFilterRow,
 * disabledLabels, the "no open X issues for this repo" Snackbar) is
 * REMOVED, replaced with a small static pill showing the app's active
 * Issue Mode — non-interactive, just a confirmation of which mode
 * you're already in. This mirrors removing the mode dropdown from Top
 * Repos, for the same reason: Mode is a single, deliberate, whole-app
 * choice made once at the gate screen, and no screen at any depth
 * offers a second way to change it. Each IssueCard shows an
 * external-link badge (see IssueCard's doc comment) since tapping one
 * opens GitHub directly, not an in-app screen.
 *
 * The "ⓘ" info icon (opening the same shared LabelInfoDialog Feed
 * uses) was removed from this screen entirely, per direct instruction,
 * along with the repoFullName subtitle under the title — this screen's
 * header is simplified down to just the repo's display title.
 * TopReposScreen keeps its own "ⓘ" for the same dialog.
 *
 * v0.46 ("Check for More Issues"), renamed and reshaped by v0.50
 * ("Refresh this repo" write-back): the LazyColumn now always ends in a
 * footer reflecting uiState.isRefreshing / hasRefreshed / refreshError
 * — an explicit "Refresh this repo" button (with a small cost note
 * underneath, "Uses one API call against your rate limit"), a small
 * inline spinner while that fetch is in flight, an inline retry row on
 * failure, or (once hasRefreshed) a small result line ABOVE the button,
 * not replacing it — v0.46's version hid the button for the rest of the
 * session once used; this one stays reusable. See
 * RepoIssuesViewModel.refreshThisRepo()'s doc comment for what this
 * closes (a real completeness gap — this screen previously had no way
 * to show more of a repo's issues than happened to be swept up while
 * Top Repos was built, or to discover closed ones) and for the write-
 * back into Top Repos' own cache. The empty-state message ("No open
 * matching issues remain.") moved INSIDE the LazyColumn as its own
 * item, rather than replacing it entirely — the footer needs to render
 * either way, so there can't meaningfully be a screen with a list
 * showing but no footer. That message can only actually be reached
 * after a refresh finds zero — repoGroup.issues is never empty on this
 * screen's initial load (see RepoIssueGroup's own doc comment).
 *
 * v0.48: isBookmarked (hardcoded false before this CM — nothing wired
 * it to anything real) now reads RepoBookmarks.isBookmarked(issue.id)
 * directly, and onToggleBookmark/snackbarHostState are wired through
 * to IssueCard's new bookmark toggle — see that file's own doc comment
 * for the full reasoning, including why this specific action uses a
 * real Snackbar rather than the Toast pattern Copy Link uses. This
 * screen gained a SnackbarHost for exactly this — it didn't have one
 * before this CM.
 *
 * v0.51 ("Hide Issue"): uiState.issues stays the RAW list — hidden
 * issues are filtered out at display time only, from
 * HiddenIssuesStore.hiddenIds (read directly here, same pattern
 * RepoBookmarks.isBookmarked() already uses on this screen), never
 * removed from the underlying state or cache. Tapping the new "✕"
 * badge calls HiddenIssuesStore.hide() directly, then shows a Snackbar
 * with an Undo action using a coroutineScope scoped to THIS Composable
 * (rememberCoroutineScope), not IssueCard's own — the card that
 * triggered it is about to disappear from the list, which would cancel
 * a scope owned by that card before Undo could ever be tapped. See
 * IssueCard's own doc comment for why this couldn't live there.
 *
 * README access went through two shapes. First, a "📄" TopAppBar action
 * opening a separate full-screen destination (RepoReadmeScreen). That
 * was replaced with ReadmeExpandableSection — an inline expand/collapse
 * widget living right in the filter row below, per the originally-
 * discussed "expansion widget at the top of the view" design, built
 * once deliberately deferred behind the IssueCard density pass. See
 * that file's own doc comment for the full reasoning, including why
 * the separate screen came out entirely rather than staying as a
 * second access point. The underlying fetch (RepoReadmeViewModel) is
 * unchanged either way — still GitHub's Contents API, still
 * deliberately NOT tracked by the RateLimitState chip this app shows
 * elsewhere, since it's a different API with a different, much larger
 * quota.
 *
 * The per-issue language tag (previously shown once per IssueCard) is
 * also gone from this screen's cards as of the same change — this app
 * currently only ever searches Kotlin issues in practice, so showing
 * it on every single card was genuinely redundant. It now appears
 * exactly ONCE, as a static pill in this screen's filter row, next to
 * the active Issue Mode pill, directly followed by the README toggle.
 *
 * The TopAppBar's actions slot briefly held the "ⓘ" info icon (moved
 * there to free up the filter row), then was replaced again — the "ⓘ"
 * came out of this screen entirely (see above), and that slot now
 * holds a single action opening the repo's main GitHub page directly
 * (https://github.com/{repoFullName}), using the same "↗" glyph/badge
 * look IssueCard's own external-link badge used before ITS glyph
 * changed to "→" (see IssueCard's doc comment, "COLLAPSIBLE CARD
 * PASS") — this header action is a distinct destination (the repo
 * itself, not any one issue), so it deliberately kept the diagonal
 * glyph rather than following that same rename.
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
    val context = LocalContext.current
    var readmeExpanded by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val visibleIssues = filterHiddenIssuesFlat(uiState.issues, HiddenIssuesStore.hiddenIds)

    // Defensive fallback only — DiscoverNavHost guarantees a mode is
    // chosen before this screen is ever reachable. See
    // IssueModeRepository.
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val activeMode = resolveActiveMode(mode)

    LaunchedEffect(repoGroup.repoFullName) {
        viewModel.loadIfNeeded(repoGroup)
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(repoGroup.repoDisplayTitle, style = MaterialTheme.typography.titleLarge)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/${repoGroup.repoFullName}"),
                                ),
                            )
                        },
                    ) {
                        Text("↗", style = MaterialTheme.typography.titleLarge)
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .background(activeMode.colorScheme().primary, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            activeMode.displayName,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer,
                                RoundedCornerShape(8.dp),
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            "Kotlin",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    ReadmeToggleButton(
                        expanded = readmeExpanded,
                        onClick = { readmeExpanded = !readmeExpanded },
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                ReadmeExpandableBody(
                    expanded = readmeExpanded,
                    repoOwner = repoGroup.repoOwner,
                    repoName = repoGroup.repoName,
                    repoFullName = repoGroup.repoFullName,
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (uiState.issues.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "No open matching issues remain.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else if (visibleIssues.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                "All matching issues in this repo are hidden.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            TextButton(onClick = onOpenHidden) {
                                Text("View Hidden")
                            }
                        }
                    }
                } else {
                    items(visibleIssues, key = { it.id }) { issue ->
                        IssueCard(
                            issue = issue,
                            isBookmarked = RepoBookmarks.isBookmarked(issue.id),
                            showExternalLinkIndicator = true,
                            onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(issue.htmlUrl)),
                                )
                            },
                            onToggleBookmark = { RepoBookmarks.toggle(context, issue) },
                            onHide = {
                                HiddenIssuesStore.hide(context, issue)
                                coroutineScope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Issue hidden",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short,
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        HiddenIssuesStore.unhide(context, issue.id)
                                    }
                                }
                            },
                            snackbarHostState = snackbarHostState,
                        )
                    }
                }
                item(key = "refreshThisRepoFooter") {
                    RefreshThisRepoFooter(
                        isRefreshing = uiState.isRefreshing,
                        hasRefreshed = uiState.hasRefreshed,
                        lastRefreshedAtMs = uiState.lastRefreshedAtMs,
                        refreshError = uiState.refreshError,
                        currentIssueCount = uiState.issues.size,
                        onRefresh = { viewModel.refreshThisRepo(context, repoGroup, language) },
                        onRetry = { viewModel.refreshThisRepo(context, repoGroup, language) },
                    )
                }
            }
        }
    }
}

/**
 * v0.46, renamed/reshaped by v0.50: the one place on this screen
 * anything can be "loading" or "errored" — see RepoIssuesUiState's own
 * doc comment for why that's scoped narrowly to this footer rather than
 * the whole screen. Deliberately a manual button, not automatic, with a
 * visible cost note — same "every network call stays a visible,
 * intentional choice" instinct Top Repos' own Load More button is built
 * around.
 *
 * UNLIKE v0.46's version: hasRefreshed no longer hides the button — the
 * idle state always shows the button (re-tappable indefinitely), and
 * ADDS a small result line above it once hasRefreshed is true ("All 7
 * open issues · checked 2m ago", or "No open issues remain · checked
 * just now" for the zero case — see the LazyColumn's own empty-state
 * item for what the MAIN list area shows in that same situation).
 */
@Composable
private fun RefreshThisRepoFooter(
    isRefreshing: Boolean,
    hasRefreshed: Boolean,
    lastRefreshedAtMs: Long?,
    refreshError: String?,
    currentIssueCount: Int,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
) {
    when {
        refreshError != null -> Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                refreshError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) {
                Text("Retry")
            }
        }
        isRefreshing -> Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp))
        }
        else -> Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (hasRefreshed) {
                val statusText = if (currentIssueCount == 0) {
                    "No open issues remain"
                } else {
                    "All $currentIssueCount open issue${if (currentIssueCount == 1) "" else "s"}"
                }
                val checkedText = lastRefreshedAtMs?.let { " · checked ${formatRelativeTime(it)}" }.orEmpty()
                Text(
                    "$statusText$checkedText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                Text("Refresh this repo")
            }
            Text(
                "Uses one API call against your rate limit.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
