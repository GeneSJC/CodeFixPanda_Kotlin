package io.github.genesjc.opensourcepanda.ui.toprepos

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.genesjc.opensourcepanda.model.LanguageFilter
import io.github.genesjc.opensourcepanda.model.RepoIssueGroup
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * The Top Repos screen: the repos with the most open issues for the active Issue Mode
 * (good first issue or help wanted), filtered by language, with a manual Load More.
 *
 * Portrait stacks the language row above the list; landscape moves the mode, repo count and
 * language picker into a left rail so the cards get the full height. Hidden issues are filtered at
 * display time: uiState keeps the raw data and the repo screen receives the raw group. The drawer
 * holds navigation, the two mode switches and, in dev builds, the animation replays.
 *
 * Not a mode dropdown in the title: the mode is one whole-app choice made at the gate screen. The
 * two "Find ..." menu items are hardcoded, so a third mode means editing them.
 * More in docs/design-notes.md, "Top Repos screen".
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
    var openDialog by remember { mutableStateOf<TopReposDialog?>(null) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var replayAnimation by remember { mutableStateOf<AnimationReplay?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val rateLimit by viewModel.rateLimit.collectAsStateWithLifecycle()
    val hasToken by viewModel.hasToken.collectAsStateWithLifecycle()
    val isDemo by viewModel.isDemo.collectAsStateWithLifecycle()
    val visibleGroups by viewModel.visibleRepoGroups.collectAsStateWithLifecycle()
    // Guaranteed non-null: DiscoverNavHost never renders this screen
    // while the mode is null (see its own doc comment).
    val currentMode by viewModel.activeMode.collectAsStateWithLifecycle()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // The list is placed differently in portrait and landscape, so it is defined once here.
    val repoList: @Composable (Modifier) -> Unit = { listModifier ->
        RepoListBody(
            uiState = uiState,
            visibleGroups = visibleGroups,
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

    TopReposDrawer(
        drawerState = drawerState,
        buildLabel = viewModel.buildLabel,
        currentMode = currentMode,
        showAllAnimations = viewModel.showDetailedDrawerMenu,
        onOpenBookmarks = onOpenBookmarks,
        onOpenHidden = onOpenHidden,
        onCheckForNewRepos = viewModel::checkForNewRepos,
        onClearCacheRequested = { openDialog = TopReposDialog.CLEAR_CACHE },
        onOpenSettings = onOpenSettings,
        onOpenWelcome = onOpenWelcome,
        onResetApp = onResetApp,
        onSelectMode = viewModel::onModeSelected,
        onAboutIssueTypes = { openDialog = TopReposDialog.LABEL_INFO },
        onReplay = { replayAnimation = it },
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            TopReposContent(
                uiState = uiState,
                visibleGroups = visibleGroups,
                currentMode = currentMode,
                isLandscape = isLandscape,
                snackbarHostState = snackbarHostState,
                onMenuClick = { scope.launch { drawerState.open() } },
                onRepoCountClick = { openDialog = TopReposDialog.RANKING_INFO },
                onLanguageSelected = viewModel::onLanguageSelected,
                headerTrailing = {
                    TopReposHeaderTrailing(
                        isDemo = isDemo,
                        rateLimit = rateLimit,
                        onDemoClick = { openDialog = TopReposDialog.TOKEN_PROBLEM },
                        // With a token problem the tap explains it instead of opening the rate-limit dialog.
                        onRateLimitClick = {
                            openDialog = if (hasToken && !uiState.tokenRejected) TopReposDialog.RATE_LIMIT else TopReposDialog.TOKEN_PROBLEM
                        },
                    )
                },
                repoList = repoList,
            )

            replayAnimation?.let { animation ->
                AnimationReplayOverlay(
                    animation = animation,
                    splashAnimation = viewModel.splashAnimation,
                    onFinished = { replayAnimation = null },
                )
            }
        }
    }

    TopReposDialogs(
        open = openDialog,
        rateLimit = rateLimit,
        hasToken = hasToken,
        onOpenSettings = onOpenSettings,
        onClearCacheConfirmed = viewModel::clearCache,
        onDismiss = { openDialog = null },
    )
}
