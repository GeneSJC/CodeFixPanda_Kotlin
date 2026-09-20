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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.githubissuefinder.model.Repo
import com.example.githubissuefinder.ui.components.LanguageFilterRow
import com.example.githubissuefinder.ui.splash.OrigamiSplashScreen
import com.example.githubissuefinder.ui.splash.ShapeStackSplashScreen
import com.example.githubissuefinder.ui.splash.SplashScreen
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * CM 1 of the "Discover" repo-first flow (option C from the architecture
 * discussion, agreed for a build-time-switchable redesign via
 * BuildConfig.LANDING_EXPERIENCE — see MainActivity.kt). A ranked Top 20
 * repos list, filterable by language via the same LanguageFilterRow Feed
 * uses (v0.27).
 *
 * The "ⓘ" beside the title (v0.28) opens a dialog explaining the ranking
 * formula. Each card also shows its actual open good-first-issue count
 * (v0.34) — "Checking open issues…" while that per-repo lookup is still
 * in flight, then the real count once it resolves. The composite score
 * does NOT yet factor this count in — that's a planned follow-up CM,
 * per the two-step plan (annotate first, re-rank/filter later).
 *
 * v0.35 adds a 60-second cooldown between loads — annotating 20 repos
 * costs 21 Search API calls per load, so a second load within the same
 * minute already risks the 30/min cap. Language chips remain tappable
 * during cooldown; a blocked tap shows a Snackbar (v0.36) — a plain
 * Material3 Snackbar rather than a literal Android Toast, since it
 * fits this screen's existing Scaffold naturally and won't queue up if
 * tapped several times in a row the way stacked system Toasts can. The
 * persistent "You can switch again in Ns" text that also existed
 * alongside the Snackbar was removed in this same CM ("new v38") as
 * redundant — the Snackbar alone covers it. See TopReposViewModel's
 * doc comment for why retry() is deliberately exempt from this
 * cooldown.
 *
 * v0.37 adds a "stars/day since creation" velocity label to each card
 * (hidden for repos under 30 days old, where it would be noise, not
 * signal) — a display-only "how fast is this catching on right now"
 * number, not currently a ranking factor. Repos above a threshold
 * (HOT_THRESHOLD, a starting guess) get a 🔥 and bolder styling.
 *
 * The hamburger menu (☰, v0.31, expanded to 3 items in v0.33) opens
 * with a non-interactive "GitHubIssueFinder {CM_VERSION}" header (new
 * in this same CM) above the animation items, so the on-device version
 * stamp is reachable from Discover the same way VersionLabel already
 * makes it reachable from the "classic" flow's TopAppBars. Below that
 * header, the menu offers "Play Origami Animation" / "Play Shape Stack
 * Animation" / "Play Coin Animation" — all three replay their
 * corresponding splash as an
 * OVERLAY drawn on top of this same composable, not by navigating away
 * to a different screen. This is deliberate: replayAnimation is local
 * state inside THIS function, so viewModel() above is only ever called
 * once per real session — toggling the overlay is just a recomposition,
 * not a disposal/recreation of TopReposScreen or its ViewModel. Language
 * filter selection, loaded repo list, and scroll position all survive a
 * replay untouched, exactly because this screen is never actually torn
 * down underneath the overlay.
 *
 * Menu presented as a simple DropdownMenu anchored to the hamburger
 * icon, not a full slide-out ModalNavigationDrawer — appropriately
 * modest for 2 actions; explicitly flagged as "get into place first,"
 * to be organized further once more menu items exist.
 *
 * As of v0.38, tapping a repo opens a real repo-scoped issue list
 * (RepoIssuesScreen), with proper back-navigation via DiscoverNavHost
 * — onRepoClick is no longer wired to a no-op. Feed/Insights/Bookmarks
 * scoping to a chosen repo remains a separate, future Commit Milestone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopReposScreen(
    onRepoClick: (Repo) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TopReposViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showInfoDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var replayAnimation by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Top Repos") },
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
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                LanguageFilterRow(
                    selected = uiState.selectedLanguage,
                    onSelect = { language ->
                        val accepted = viewModel.onLanguageSelected(language)
                        if (!accepted) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    "Please wait ${uiState.cooldownSecondsRemaining}s before switching again",
                                )
                            }
                        }
                    },
                )

                when {
                    uiState.noTokenConfigured -> CenteredMessage(
                        text = "This screen needs a live GitHub connection — add a token to " +
                            "local.properties to see ranked repos here. No sample-data " +
                            "fallback yet for this screen.",
                    )
                    uiState.isLoading && uiState.repos.isEmpty() -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                    uiState.errorMessage != null && uiState.repos.isEmpty() -> Box(
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
                    else -> LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(uiState.repos, key = { it.id }) { repo ->
                            RepoCard(repo = repo, onClick = { onRepoClick(repo) })
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
                    "Each repo gets a combined score, shown as the number on its card, " +
                        "based on four things:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                Text(
                    "• Stars, relative to others in this list — the biggest single factor.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Text(
                    "• Forks — a sign the project is actually being built on, not just " +
                        "bookmarked.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Text(
                    "• How recently it was updated — very recent activity counts more, " +
                        "fading out over about a year.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Text(
                    "• Open issue count — a smaller number scores slightly better, since a " +
                        "huge backlog can signal an overwhelmed or stalled project.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                Text(
                    "This reflects general repo health. Each card also shows its actual " +
                        "count of currently-open good-first-issues directly (or \"Checking " +
                        "open issues…\" while that's still loading) — the score above doesn't " +
                        "factor that count in yet, but you can see both at a glance. A future " +
                        "update will let you re-rank by open-issue count directly.",
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

@Composable
private fun RepoCard(repo: Repo, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    repo.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        "${(repo.score * 100).toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }

            Text(
                repo.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("⭐ ${repo.stars}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("🍴 ${repo.forks}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${repo.openIssuesCount} open issues", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(repo.updatedAtLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            GoodFirstIssueCountLabel(repo.goodFirstIssueCount)
            StarsPerDayLabel(repo.starsPerDay)
        }
    }
}

@Composable
private fun GoodFirstIssueCountLabel(count: Int?) {
    val (text, color) = when {
        count == null -> "Checking open issues…" to MaterialTheme.colorScheme.onSurfaceVariant
        count == 0 -> "No open good-first-issues" to MaterialTheme.colorScheme.onSurfaceVariant
        count == 1 -> "1 good-first-issue open" to MaterialTheme.colorScheme.primary
        else -> "$count good-first-issues open" to MaterialTheme.colorScheme.primary
    }
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        modifier = Modifier.padding(top = 6.dp),
    )
}

/**
 * "Stars per day since creation" — a velocity signal, not a ranking
 * factor (see RepoScoring.kt's doc comment). Null means the repo is
 * too new (under 30 days old) for the number to mean anything yet, so
 * nothing is shown at all rather than a misleading placeholder.
 *
 * HOT_THRESHOLD is a starting guess, not a tuned constant — easy to
 * adjust once this is seen against real data.
 */
@Composable
private fun StarsPerDayLabel(starsPerDay: Float?) {
    if (starsPerDay == null) return

    val isHot = starsPerDay >= HOT_THRESHOLD
    val formatted = if (starsPerDay >= 10f) {
        "${starsPerDay.toInt()}"
    } else {
        String.format(Locale.US, "%.1f", starsPerDay)
    }

    Text(
        if (isHot) "🔥 $formatted stars/day" else "$formatted stars/day",
        style = MaterialTheme.typography.bodySmall,
        color = if (isHot) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = if (isHot) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier.padding(top = 4.dp),
    )
}

private const val HOT_THRESHOLD = 50f
