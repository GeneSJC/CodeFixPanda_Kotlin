package com.example.githubissuefinder.ui.discover

import com.example.githubissuefinder.BuildConfig

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.githubissuefinder.data.formatRelativeTime
import com.example.githubissuefinder.model.IssueLabelFilter
import com.example.githubissuefinder.model.RepoIssueGroup
import com.example.githubissuefinder.ui.components.LanguageFilterRow
import com.example.githubissuefinder.ui.splash.OrigamiSplashScreen
import com.example.githubissuefinder.ui.splash.ShapeStackSplashScreen
import com.example.githubissuefinder.ui.splash.SplashScreen
import com.example.githubissuefinder.ui.splash.TitleRevealSplashScreen
import kotlinx.coroutines.flow.collect

/**
 * CM 1 of the "Discover" repo-first flow (option C from the architecture
 * discussion, agreed for a build-time-switchable redesign via
 * BuildConfig.LANDING_EXPERIENCE — see MainActivity.kt).
 *
 * v0.42 ("the data-layer swap"): this screen no longer shows a ranked
 * list of REPOS — it shows repos as they fall out of grouping actual
 * matching ISSUES by their parent repo. See TopReposViewModel and
 * RepoIssueGroup's doc comments for the full reasoning. Concretely,
 * this means:
 *  - Each card shows a match count (RepoIssueGroup.matchCount),
 *    replacing the old composite score badge — sorted descending, the
 *    new ranking signal.
 *  - Stars, forks, open-issue count, "updated X ago," and the
 *    stars/day velocity label are ALL GONE — none of that data exists
 *    in an issue-search response. RepoCard is now a much simpler card
 *    than it used to be; this is a real, deliberate reduction in
 *    richness, not a bug (see RepoIssueGroup's doc comment).
 *  - The "ⓘ" ranking dialog's text is rewritten to describe
 *    match-count ranking, replacing the old 4-factor composite-score
 *    explanation, which is no longer true of anything this screen
 *    does.
 *  - No more "Checking open issues…" loading state per card — the
 *    match count is already final and correct the instant the list
 *    appears, since it's built directly from the issues already in
 *    hand, not a separate async per-repo lookup running after the
 *    fact.
 *  - No more cooldown, no more blocked-switch Snackbar: switching
 *    language or mode is a single Search API call now, not ~21 — see
 *    TopReposViewModel's doc comment. SnackbarHost/SnackbarHostState
 *    are removed from this screen entirely since nothing shows one
 *    anymore.
 *
 * The hamburger menu (☰, v0.31, expanded to 3 items in v0.33, 4 items
 * once "Play Title Reveal Animation" was added) opens with a
 * non-interactive "GitHubIssueFinder {CM_VERSION}" header above the
 * animation items, so the on-device version stamp is reachable from
 * Discover the same way VersionLabel makes it reachable from the
 * "classic" flow's TopAppBars. Below that header, the menu offers
 * "Play Origami Animation" / "Play Shape Stack Animation" / "Play Coin
 * Animation" / "Play Title Reveal Animation" — all four replay their
 * corresponding splash as an OVERLAY drawn on top of this same
 * composable, not by navigating away — replayAnimation is local state
 * inside THIS function, so viewModel()
 * above is only ever called once per real session.
 *
 * As of v0.38, tapping a repo opens a real repo-scoped issue list
 * (RepoIssuesScreen), via DiscoverNavHost. As of v0.42, that navigation
 * passes a RepoIssueGroup instead of a Repo — see DiscoverNavHost.
 *
 * v0.41 ("Issue Mode" shell), THEN REMOVED IN THIS SAME CM: the
 * static "Top Repos" title was briefly a mode dropdown, letting you
 * switch modes without leaving this screen. Decided against, once
 * seen running: Mode is a single, deliberate, whole-app choice made
 * once at the gate screen (IssueModeGateScreen), and no screen at any
 * depth should offer a second way to change it — a quiet dropdown
 * living inside a title bar undercuts that "you have to set it"
 * framing, and in practice it wasn't even discoverable (missed
 * entirely on first use, confirmed on a running device). The title is
 * now a plain, non-interactive Text(currentMode.displayName) — same
 * removal, same reasoning, applied to RepoIssuesScreen's own
 * in-screen label picker too (see that file).
 *
 * v0.43 (pagination): the list now ends in a footer row (a plain
 * `item { }`, not part of the `items()` block) reflecting
 * uiState.canLoadMore / isLoadingMore / loadMoreError — a "Load More"
 * button, a small inline spinner while that fetch is in flight, an
 * inline retry row if it failed (the existing list stays exactly as
 * it was either way — only the footer changes), or a plain "that's
 * everything" line once GitHub genuinely has nothing further for this
 * query. See TopReposViewModel's doc comment for the cross-page
 * merge/dedup this relies on — a repo whose matches span two pages
 * updates the SAME card here, it doesn't appear a second time.
 *
 * ALSO IN v0.43: two explicitly-worded items now sit at the bottom of
 * the hamburger menu — "Find Good First Issues" and "Find Help Wanted
 * Issues" — calling viewModel.onModeSelected(mode) directly, switching
 * the active Issue Mode in place without leaving this screen. This is
 * NOT a resurrection of the old v0.41 title-bar dropdown, despite
 * switching Mode from within Top Repos either way: that dropdown sat
 * directly in the title, visible and tappable without any deliberate
 * action, which is exactly what made it feel accidental rather than a
 * real choice, and why it was removed. Living at the bottom of a menu
 * the user has to deliberately open keeps the "this is a real,
 * considered choice" framing intact.
 *
 * A gate-screen-based design (a menu item navigating back to
 * IssueModeGateScreen, popping this screen off the back stack first so
 * the stack didn't accumulate stale entries) was fully worked out
 * first, then deliberately set aside in favor of this simpler,
 * direct-switch version — reusing the gate would be the more scalable
 * choice if a third mode is ever added (IssueModeGateScreen already
 * loops IssueLabelFilter.entries, so a third mode shows up there for
 * free), but wasn't worth the added navigation complexity for exactly
 * two modes. Worth remembering if a third mode does show up: THIS
 * file's two menu items are hardcoded, not generated from
 * IssueLabelFilter.entries, and would need direct edits — they won't
 * grow to cover a third mode on their own the way the gate screen
 * would.
 *
 * v0.44: two small additions. First, a "Repos" caption now sits under
 * the mode name in the title — added after a real mix-up (confirmed on
 * a running device) between this screen's Load More and
 * RepoIssuesScreen's own, unrelated, unpaginated list further in;
 * cheap, direct fix for that specific confusion rather than a deeper
 * redesign. Second, SnackbarHost is back (removed in v0.42 when the
 * old cooldown Snackbar became unnecessary) — for a new, unrelated
 * reason this time: TopReposViewModel.snackbarEvents delivers a
 * one-shot message when a Load More hits GitHub's Search API rate
 * limit specifically (see that class's doc comment for why this is
 * scoped to Load More only, and why it's a Channel-backed one-shot
 * event rather than a UiState field).
 *
 * v0.45 (persistent cache): a small "Updated X ago" line now sits
 * under the language filter row, driven by uiState.lastUpdatedAtMs —
 * the only visible sign, most of the time, that this screen might be
 * showing cached data rather than something just fetched (see
 * TopReposViewModel and RepoIssueGroupCache doc comments). Two more
 * hamburger-menu items sit above the mode-switch pair, grouped with
 * the animation items via their own divider: "Refresh" (forces a live
 * fetch for the current language/mode, bypassing and then overwriting
 * the cache) and "Clear Cache" (wipes the ENTIRE persistent cache, not
 * just the active combination — see RepoIssueGroupCache.clearAll()'s
 * doc comment for why that's a known, named limitation rather than an
 * oversight).
 *
 * v0.46: a small chip in the top bar's actions slot (next to the "ⓘ"
 * icon) shows how many repo cards are currently loaded — styled
 * directly after Feed's own RateLimitChip, the same reference point
 * this was asked for by name. Just reads uiState.repoGroups.size
 * directly; no separate tracking needed, updates for free on every
 * Load More and every mode/language switch.
 *
 * v0.48: added "Bookmarks" as the very first menu item, right after
 * the version header — a real navigation destination (see
 * DiscoverNavHost's new ROUTE_REPO_BOOKMARKS and RepoBookmarksScreen),
 * placed ahead of the animation/settings-style items below it since
 * it's the one item in this menu someone might actually want to reach
 * repeatedly, not a one-off action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopReposScreen(
    onRepoClick: (RepoIssueGroup) -> Unit,
    onOpenBookmarks: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TopReposViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showInfoDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var replayAnimation by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    // Guaranteed non-null: DiscoverNavHost never renders this screen
    // while IssueMode.current is null (see its own doc comment).
    val currentMode = IssueMode.current ?: IssueLabelFilter.GOOD_FIRST_ISSUE

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(currentMode.displayName)
                            Text(
                                "Repos",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    navigationIcon = {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Text("☰", style = MaterialTheme.typography.titleLarge)
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                            ) {
                                Text(
                                    "GitHubIssueFinder ${BuildConfig.CM_VERSION}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Bookmarks") },
                                    onClick = {
                                        showMenu = false
                                        onOpenBookmarks()
                                    },
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Play Origami Animation") },
                                    onClick = {
                                        showMenu = false
                                        replayAnimation = "origami"
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Play Shape Stack Animation") },
                                    onClick = {
                                        showMenu = false
                                        replayAnimation = "shapeStack"
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Play Coin Animation") },
                                    onClick = {
                                        showMenu = false
                                        replayAnimation = "coin"
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Play Title Reveal Animation") },
                                    onClick = {
                                        showMenu = false
                                        replayAnimation = "titleReveal"
                                    },
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Refresh") },
                                    onClick = {
                                        showMenu = false
                                        viewModel.refresh()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Clear Cache") },
                                    onClick = {
                                        showMenu = false
                                        viewModel.clearCache()
                                    },
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Find Good First Issues") },
                                    onClick = {
                                        showMenu = false
                                        viewModel.onModeSelected(IssueLabelFilter.GOOD_FIRST_ISSUE)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Find Help Wanted Issues") },
                                    onClick = {
                                        showMenu = false
                                        viewModel.onModeSelected(IssueLabelFilter.HELP_WANTED)
                                    },
                                )
                            }
                        }
                    },
                    actions = {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.padding(end = 8.dp),
                        ) {
                            Text(
                                repoCountLabel(uiState.repoGroups.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                        Text(
                            "ⓘ",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { showInfoDialog = true }
                                .padding(end = 16.dp),
                        )
                    },
                )
            },
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                LanguageFilterRow(
                    selected = uiState.selectedLanguage,
                    onSelect = viewModel::onLanguageSelected,
                )

                uiState.lastUpdatedAtMs?.let { updatedAtMs ->
                    Text(
                        "Updated ${formatRelativeTime(updatedAtMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }

                when {
                    uiState.noTokenConfigured -> CenteredMessage(
                        text = "This screen needs a live GitHub connection — add a token to " +
                            "local.properties to see ranked repos here. No sample-data " +
                            "fallback yet for this screen.",
                    )
                    uiState.isLoading && uiState.repoGroups.isEmpty() -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                    uiState.errorMessage != null && uiState.repoGroups.isEmpty() -> Box(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(uiState.errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
                            Button(onClick = viewModel::retry, modifier = Modifier.padding(top = 12.dp)) {
                                Text("Retry")
                            }
                        }
                    }
                    uiState.repoGroups.isEmpty() -> CenteredMessage(
                        text = "No repos with an open ${currentMode.displayName} issue found for " +
                            "this language right now.",
                    )
                    else -> LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(uiState.repoGroups, key = { it.repoFullName }) { group ->
                            RepoCard(group = group, onClick = { onRepoClick(group) })
                        }
                        item(key = "loadMoreFooter") {
                            LoadMoreFooter(
                                isLoadingMore = uiState.isLoadingMore,
                                canLoadMore = uiState.canLoadMore,
                                loadMoreError = uiState.loadMoreError,
                                onLoadMore = viewModel::loadMore,
                                onRetry = viewModel::retryLoadMore,
                            )
                        }
                    }
                }
            }
        }

        if (replayAnimation != null) {
            val onFinished = { replayAnimation = null }
            when (replayAnimation) {
                "origami" -> OrigamiSplashScreen(onFinished = onFinished)
                "shapeStack" -> ShapeStackSplashScreen(onFinished = onFinished)
                "titleReveal" -> TitleRevealSplashScreen(onFinished = onFinished)
                else -> SplashScreen(onFinished = onFinished)
            }
        }
    }

    if (showInfoDialog) {
        RankingInfoDialog(onDismiss = { showInfoDialog = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RankingInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Got it") }
        },
        title = { Text("How are these repos ranked?") },
        text = {
            Column {
                Text(
                    "This list is built directly from matching issues, not from a separate " +
                        "repo search — every repo shown is guaranteed to have at least one " +
                        "open issue for whichever mode you're in.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                Text(
                    "The number on each card is simply how many matching issues that repo " +
                        "had in this batch — repos with more matches sort higher. It isn't a " +
                        "measure of the repo's overall popularity or health (stars, forks, " +
                        "how recently it was updated) the way an earlier version of this " +
                        "screen used — that data isn't available without extra lookups this " +
                        "version deliberately doesn't make yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                Text(
                    "A future update may add that context back for repos worth a closer look, " +
                        "without giving up the guarantee that every repo shown here actually " +
                        "has something open for you.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
private fun CenteredMessage(text: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * v0.42: rewritten from the ground up for RepoIssueGroup instead of
 * Repo. Deliberately simple compared to the old card — title, path,
 * and a match-count badge is genuinely all there is to show now (see
 * this file's top doc comment for what's gone and why).
 */
@Composable
private fun RepoCard(group: RepoIssueGroup, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        group.repoDisplayTitle,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        group.repoFullName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        matchCountLabel(group.matchCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }
    }
}

private fun matchCountLabel(count: Int): String = if (count == 1) "1 match" else "$count matches"

/**
 * v0.46: the small top-bar chip showing how many repo cards are
 * currently loaded — styled directly after Feed's existing
 * RateLimitChip (same Surface/RoundedCornerShape/secondaryContainer/
 * bodySmall shape), placed in the same actions slot. Counts
 * uiState.repoGroups.size directly — already exactly "how many repos
 * are loaded," no separate tracking needed. Updates for free on every
 * Load More and every mode/language switch, since it just reads
 * whatever's already in uiState.
 */
private fun repoCountLabel(count: Int): String = if (count == 1) "1 repo" else "$count repos"

/**
 * v0.43: the pagination footer — one of four states, exactly one shown
 * at a time. Deliberately a manual "Load More" tap rather than
 * auto-loading on scroll: an explicit action keeps the "one Search API
 * call per fetch" cost visible and intentional, the same instinct
 * behind this whole redesign not spending calls the user didn't ask
 * for.
 */
@Composable
private fun LoadMoreFooter(
    isLoadingMore: Boolean,
    canLoadMore: Boolean,
    loadMoreError: String?,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
) {
    when {
        loadMoreError != null -> Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                loadMoreError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) {
                Text("Retry")
            }
        }
        isLoadingMore -> Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp))
        }
        canLoadMore -> Button(onClick = onLoadMore, modifier = Modifier.fillMaxWidth()) {
            Text("Load More")
        }
        else -> Text(
            "That's everything GitHub has for this search.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
    }
}

