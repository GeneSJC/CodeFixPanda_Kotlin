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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.example.githubissuefinder.model.IssueLabelFilter
import com.example.githubissuefinder.model.RepoIssueGroup
import com.example.githubissuefinder.ui.components.LanguageFilterRow
import com.example.githubissuefinder.ui.splash.OrigamiSplashScreen
import com.example.githubissuefinder.ui.splash.ShapeStackSplashScreen
import com.example.githubissuefinder.ui.splash.SplashScreen

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
 * The hamburger menu (☰, v0.31, expanded to 3 items in v0.33) opens
 * with a non-interactive "GitHubIssueFinder {CM_VERSION}" header above
 * the animation items, so the on-device version stamp is reachable
 * from Discover the same way VersionLabel makes it reachable from the
 * "classic" flow's TopAppBars. Below that header, the menu offers
 * "Play Origami Animation" / "Play Shape Stack Animation" / "Play Coin
 * Animation" — all three replay their corresponding splash as an
 * OVERLAY drawn on top of this same composable, not by navigating away
 * — replayAnimation is local state inside THIS function, so viewModel()
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
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopReposScreen(
    onRepoClick: (RepoIssueGroup) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TopReposViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showInfoDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var replayAnimation by remember { mutableStateOf<String?>(null) }
    // Guaranteed non-null: DiscoverNavHost never renders this screen
    // while IssueMode.current is null (see its own doc comment).
    val currentMode = IssueMode.current ?: IssueLabelFilter.GOOD_FIRST_ISSUE

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(currentMode.displayName) },
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
                            }
                        }
                    },
                    actions = {
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
                    }
                }
            }
        }

        if (replayAnimation != null) {
            val onFinished = { replayAnimation = null }
            when (replayAnimation) {
                "origami" -> OrigamiSplashScreen(onFinished = onFinished)
                "shapeStack" -> ShapeStackSplashScreen(onFinished = onFinished)
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

