package io.github.genesjc.githubissuefinder.ui.discover

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.genesjc.githubissuefinder.data.filterHiddenIssues
import io.github.genesjc.githubissuefinder.data.resolveActiveMode
import io.github.genesjc.githubissuefinder.model.LanguageFilter
import io.github.genesjc.githubissuefinder.model.RepoIssueGroup
import io.github.genesjc.githubissuefinder.ui.ads.AdBannerSlot
import io.github.genesjc.githubissuefinder.ui.ads.AdFreeOfferLine
import io.github.genesjc.githubissuefinder.ui.components.DemoBanner
import io.github.genesjc.githubissuefinder.ui.components.LabelInfoDialog
import io.github.genesjc.githubissuefinder.ui.components.LanguageFilterRow
import io.github.genesjc.githubissuefinder.ui.components.ModePill
import io.github.genesjc.githubissuefinder.ui.findActivity
import kotlinx.coroutines.flow.collect

/**
 * The Top Repos screen: the repos with the most open issues for the active Issue Mode
 * (good first issue or help wanted), filtered by language, with a manual Load More.
 *
 * Portrait stacks the language row above the list. Landscape puts the mode, repo count
 * and language picker in a narrow left rail so the card list gets the full height.
 * Hidden issues are filtered at display time; the raw data stays in uiState, and the
 * repo screen receives the raw group. The hamburger menu holds navigation, the two
 * mode switches and, in dev builds, the animation replays.
 *
 * Deliberately not a mode dropdown in the title: the mode is one whole-app choice made
 * at the gate screen. The two "Find ..." menu items are hardcoded, so a third mode
 * means editing them. More in docs/design-notes.md, "Top Repos screen".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopReposScreen(
    onRepoClick: (RepoIssueGroup, LanguageFilter) -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHidden: () -> Unit,
    onOpenSettings: () -> Unit,
    onResetApp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TopReposViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showInfoDialog by remember { mutableStateOf(false) }
    var showLabelInfoDialog by remember { mutableStateOf(false) }
    var showRateLimitDialog by remember { mutableStateOf(false) }
    var showTokenProblemDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showClearCacheConfirm by remember { mutableStateOf(false) }
    var replayAnimation by remember { mutableStateOf<AnimationReplay?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val rateLimit by viewModel.rateLimit.collectAsStateWithLifecycle()
    val hasToken by viewModel.hasToken.collectAsStateWithLifecycle()
    val isDemo by viewModel.isDemo.collectAsStateWithLifecycle()
    val privacyOptionsRequired by viewModel.privacyOptionsRequired.collectAsStateWithLifecycle()
    val canShowAds by viewModel.canShowAds.collectAsStateWithLifecycle()
    val adFreeUntilMs by viewModel.adFreeUntilMs.collectAsStateWithLifecycle()
    val canOfferAdFree by viewModel.canOfferAdFree.collectAsStateWithLifecycle()
    val activity = LocalContext.current.findActivity()
    val hiddenIds by viewModel.hiddenIds.collectAsStateWithLifecycle()
    val hiddenGroups = filterHiddenIssues(uiState.repoGroups, hiddenIds)
    // Guaranteed non-null: DiscoverNavHost never renders this screen
    // while the mode is null (see its own doc comment).
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val currentMode = resolveActiveMode(mode)
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // The list is placed differently in portrait and landscape, so it is defined once here.
    val repoList: @Composable (Modifier) -> Unit = { listModifier ->
        RepoListBody(
            uiState = uiState,
            hiddenGroups = hiddenGroups,
            snippets = viewModel.snippets,
            currentMode = currentMode,
            onRetry = viewModel::retry,
            onLoadMore = viewModel::loadMore,
            onRetryLoadMore = viewModel::retryLoadMore,
            onRepoCardShown = { group -> viewModel.onRepoCardShown(group.repoOwner, group.repoName, group.repoFullName) },
            onRepoClick = onRepoClick,
            modifier = listModifier,
        )
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Re-read the cache on every fresh composition, including on return from the repo screen,
    // whose "Refresh this repo" writes straight to the cache.
    LaunchedEffect(Unit) {
        viewModel.syncFromCache()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = { AdBannerSlot(visible = canShowAds, modifier = Modifier.navigationBarsPadding()) },
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            // In landscape this content lives in the left rail instead.
                            if (!isLandscape) {
                                Column {
                                    ModePill(mode = currentMode, onInfoClick = { showLabelInfoDialog = true })
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 4.dp),
                                    ) {
                                        RepoCountAndUpdated(repoCount = hiddenGroups.size, lastUpdatedAtMs = uiState.lastUpdatedAtMs)
                                    }
                                }
                            }
                        },
                        navigationIcon = {
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Text("☰", style = MaterialTheme.typography.titleLarge)
                                }
                                TopReposMenu(
                                    expanded = showMenu,
                                    onDismiss = { showMenu = false },
                                    buildLabel = viewModel.buildLabel,
                                    showAllAnimations = viewModel.showDetailedDrawerMenu,
                                    onOpenBookmarks = onOpenBookmarks,
                                    onOpenHidden = onOpenHidden,
                                    onCheckForNewRepos = viewModel::checkForNewRepos,
                                    onClearCache = { showClearCacheConfirm = true },
                                    onOpenSettings = onOpenSettings,
                                    onResetApp = onResetApp,
                                    showPrivacyOptions = privacyOptionsRequired,
                                    onPrivacyOptions = { activity?.let(viewModel::onPrivacyOptions) },
                                    showAdFreeOffer = canOfferAdFree,
                                    onAdFreeOffer = { activity?.let(viewModel::onAdFreeOffer) },
                                    onSelectMode = viewModel::onModeSelected,
                                    onReplay = { replayAnimation = it },
                                )
                            }
                        },
                        actions = {
                            // Always shown (a placeholder with no data yet) so the title bar does not shift. With a token
                            // problem the tap explains it instead of opening the rate-limit dialog.
                            // The rate-limit chip is on the top row, level with the mode pill, and the ad-free offer sits
                            // below it, level with the repo count. In the open build the line draws nothing.
                            Column(horizontalAlignment = Alignment.End) {
                                RateLimitChip(
                                    info = rateLimit,
                                    onClick = {
                                        if (hasToken && !uiState.tokenRejected) {
                                            showRateLimitDialog = true
                                        } else {
                                            showTokenProblemDialog = true
                                        }
                                    },
                                )
                                AdFreeOfferLine(
                                    adFreeUntilMs = adFreeUntilMs,
                                    canOffer = canOfferAdFree,
                                    onOffer = { activity?.let(viewModel::onAdFreeOffer) },
                                    modifier = Modifier.padding(end = 12.dp, top = 2.dp),
                                )
                            } else {
                                            showTokenProblemDialog = true
                                        }
                                    },
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
                    if (isDemo) DemoBanner(onEnterToken = onOpenSettings)
                }
            },
        ) { padding ->
            if (isLandscape) {
                // Landscape: a left rail (mode, count, language picker) beside the card list, so the list
                // gets the full height.
                Row(modifier = Modifier.padding(padding).fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .width(180.dp)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        ModePill(mode = currentMode, onInfoClick = { showLabelInfoDialog = true })
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 8.dp),
                        ) {
                            RepoCountAndUpdated(repoCount = hiddenGroups.size, lastUpdatedAtMs = uiState.lastUpdatedAtMs)
                        }
                        LanguageFilterRow(
                            selected = uiState.selectedLanguage,
                            onSelect = viewModel::onLanguageSelected,
                            wrap = true,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                    repoList(Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                Column(modifier = Modifier.padding(padding)) {
                    LanguageFilterRow(
                        selected = uiState.selectedLanguage,
                        onSelect = viewModel::onLanguageSelected,
                    )
                    repoList(Modifier.fillMaxSize())
                }
            }
        }

        replayAnimation?.let { animation ->
            AnimationReplayOverlay(
                animation = animation,
                splashAnimation = viewModel.splashAnimation,
                onFinished = { replayAnimation = null },
            )
        }
    }

    if (showInfoDialog) {
        RankingInfoDialog(onDismiss = { showInfoDialog = false })
    }

    if (showLabelInfoDialog) {
        LabelInfoDialog(onDismiss = { showLabelInfoDialog = false })
    }

    if (showRateLimitDialog) {
        RateLimitDialog(
            info = rateLimit,
            onDismiss = { showRateLimitDialog = false },
        )
    }

    if (showTokenProblemDialog) {
        TokenProblemDialog(
            tokenRejected = hasToken,
            onOpenSettings = onOpenSettings,
            onDismiss = { showTokenProblemDialog = false },
        )
    }

    if (showClearCacheConfirm) {
        ClearCacheDialog(
            onConfirm = {
                showClearCacheConfirm = false
                viewModel.clearCache()
            },
            onDismiss = { showClearCacheConfirm = false },
        )
    }
}
