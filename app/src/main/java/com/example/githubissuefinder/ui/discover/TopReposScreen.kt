package com.example.githubissuefinder.ui.discover

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
import com.example.githubissuefinder.model.Repo
import com.example.githubissuefinder.ui.components.LanguageFilterRow
import com.example.githubissuefinder.ui.splash.OrigamiSplashScreen
import com.example.githubissuefinder.ui.splash.SplashScreen

/**
 * CM 1 of the "Discover" repo-first flow (option C from the architecture
 * discussion, agreed for a build-time-switchable redesign via
 * BuildConfig.LANDING_EXPERIENCE — see MainActivity.kt). A ranked Top 20
 * repos list, filterable by language via the same LanguageFilterRow Feed
 * uses (v0.27).
 *
 * The "ⓘ" beside the title (v0.28) opens a dialog explaining the ranking
 * formula. The hamburger menu (☰, v0.31) offers "Play Origami Animation"
 * / "Play Coin Animation" — both replay the corresponding splash as an
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
 * Tapping a repo is intentionally a no-op for now (onRepoClick is wired
 * to an empty lambda in MainActivity) — scoped Feed/Insights/Bookmarks
 * for a chosen repo are deliberately deferred to later Commit
 * Milestones, per the incremental plan agreed for this feature. Rows
 * still look and feel tappable (Card's built-in ripple) so the
 * interaction reads as "not yet wired" rather than broken, once drill-in
 * is added.
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
                                DropdownMenuItem(
                                    text = { Text("Play Origami Animation") },
                                    onClick = {
                                        showMenu = false
                                        replayAnimation = "origami"
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
                    "This reflects general repo health, not whether it actually has open " +
                        "good-first-issues right now — GitHub's search doesn't support " +
                        "filtering repos by that directly.",
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
        }
    }
}
