package io.github.genesjc.opensourcepanda.ui.discover

import android.content.res.Configuration
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.genesjc.opensourcepanda.data.filterHiddenIssues
import io.github.genesjc.opensourcepanda.data.resolveActiveMode
import io.github.genesjc.opensourcepanda.model.LanguageFilter
import io.github.genesjc.opensourcepanda.model.RepoIssueGroup
import io.github.genesjc.opensourcepanda.ui.ads.AdBannerSlot
import io.github.genesjc.opensourcepanda.ui.ads.AdFreeOfferLine
import io.github.genesjc.opensourcepanda.ui.components.LabelInfoDialog
import io.github.genesjc.opensourcepanda.ui.components.LanguageFilterRow
import io.github.genesjc.opensourcepanda.ui.components.ModePill
import io.github.genesjc.opensourcepanda.ui.findActivity
import io.github.genesjc.opensourcepanda.ui.components.BrandBarIconButton
import io.github.genesjc.opensourcepanda.ui.components.BrandSoftBackground
import io.github.genesjc.opensourcepanda.ui.components.BrandHeader
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
    onOpenWelcome: () -> Unit,
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
        BrandSoftBackground {
            Scaffold(
                containerColor = Color.Transparent,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = { AdBannerSlot(visible = canShowAds, modifier = Modifier.navigationBarsPadding()) },
                topBar = {
                    BrandHeader(
                        leading = {
                            Box {
                                BrandBarIconButton(onClick = { showMenu = true }) {
                                    // The text-presentation selector keeps the device from drawing its own dark emoji glyph.
                                    Text("☰\uFE0E", style = MaterialTheme.typography.titleLarge)
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
                                    onOpenWelcome = onOpenWelcome,
                                    onResetApp = onResetApp,
                                    showPrivacyOptions = privacyOptionsRequired,
                                    onPrivacyOptions = { activity?.let(viewModel::onPrivacyOptions) },
                                    showAdFreeOffer = canOfferAdFree,
                                    onAdFreeOffer = { activity?.let(viewModel::onAdFreeOffer) },
                                    onSelectMode = viewModel::onModeSelected,
                                    onAboutIssueTypes = { showLabelInfoDialog = true },
                                    onReplay = { replayAnimation = it },
                                )
                            }
                        },
                        // In landscape the mode and the count live in the left rail instead.
                        center = {
                            if (!isLandscape) {
                                ModeTitle(
                                    mode = currentMode,
                                    repoCount = hiddenGroups.size,
                                    onRepoCountClick = { showInfoDialog = true },
                                    modifier = Modifier.width(HEADER_PILL_WIDTH),
                                )
                            }
                        },
                        trailing = {
                            if (!isLandscape) {
                                UpdatedText(uiState.lastUpdatedAtMs, modifier = Modifier.padding(end = 8.dp), onBar = true)
                            }
                            // Under the updated line, on the same right edge. Always shown (a placeholder with no data yet) so
                            // the header does not shift. With no token there are no rate-limit numbers, so the demo marker takes
                            // its place, and with a token problem the tap explains it instead of opening the rate-limit dialog.
                            Box(modifier = Modifier.padding(top = 6.dp, end = 8.dp)) {
                                if (isDemo) {
                                    DemoChip(onClick = { showTokenProblemDialog = true })
                                } else {
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
                                }
                            }
                            // The ad-free offer, under the count; in the open build the line draws nothing.
                            AdFreeOfferLine(
                                adFreeUntilMs = adFreeUntilMs,
                                canOffer = canOfferAdFree,
                                onOffer = { activity?.let(viewModel::onAdFreeOffer) },
                                modifier = Modifier.padding(end = 8.dp, top = 2.dp),
                            )
                        },
                    )
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
                            ModePill(mode = currentMode)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 8.dp),
                            ) {
                                RepoCountAndUpdated(
                                    repoCount = hiddenGroups.size,
                                    lastUpdatedAtMs = uiState.lastUpdatedAtMs,
                                    onCountClick = { showInfoDialog = true },
                                )
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

/** The mode pill in the header, which works as the screen's title: a fixed width, so both modes line up and a long name wraps. */
private val HEADER_PILL_WIDTH = 180.dp
