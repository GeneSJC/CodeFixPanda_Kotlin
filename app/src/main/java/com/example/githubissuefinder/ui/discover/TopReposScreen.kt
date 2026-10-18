package com.example.githubissuefinder.ui.discover


import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.githubissuefinder.data.RateLimitInfo
import com.example.githubissuefinder.data.filterHiddenIssues
import com.example.githubissuefinder.data.formatRelativeTime
import com.example.githubissuefinder.data.resolveActiveMode
import com.example.githubissuefinder.model.IssueLabelFilter
import com.example.githubissuefinder.model.LanguageFilter
import com.example.githubissuefinder.model.RepoIssueGroup
import com.example.githubissuefinder.ui.components.LabelInfoDialog
import com.example.githubissuefinder.ui.components.LanguageFilterRow
import com.example.githubissuefinder.ui.components.VersionLabel
import com.example.githubissuefinder.ui.theme.colorScheme
import com.example.githubissuefinder.ui.settings.ApiTokenHelpDialog
import com.example.githubissuefinder.ui.splash.AppStartSplashScreen
import com.example.githubissuefinder.ui.splash.OrigamiSplashScreen
import com.example.githubissuefinder.ui.splash.ShapeStackSplashScreen
import com.example.githubissuefinder.ui.splash.SplashScreen
import com.example.githubissuefinder.ui.splash.TitleRevealSplashScreen
import com.example.githubissuefinder.ui.theme.DrawerSectionFindIssues
import com.example.githubissuefinder.ui.theme.DrawerSectionRepoManagement
import com.example.githubissuefinder.ui.theme.DrawerSectionSandbox
import kotlin.math.cos
import kotlin.math.sin
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
    var replayAnimation by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val rateLimit by viewModel.rateLimit.collectAsStateWithLifecycle()
    val hasToken by viewModel.hasToken.collectAsStateWithLifecycle()
    val hiddenIds by viewModel.hiddenIds.collectAsStateWithLifecycle()
    val hiddenGroups = filterHiddenIssues(uiState.repoGroups, hiddenIds)
    // Guaranteed non-null: DiscoverNavHost never renders this screen
    // while the mode is null (see its own doc comment).
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val currentMode = resolveActiveMode(mode)
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Hoisted so the portrait title bar and the landscape rail can both render it.
    val modePill: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .background(currentMode.colorScheme().primary, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    currentMode.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                )
                // Only "good first issue" gets an info icon; it reads less clearly than "help wanted".
                if (currentMode == IssueLabelFilter.GOOD_FIRST_ISSUE) {
                    Text(
                        "ⓘ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .clickable { showLabelInfoDialog = true },
                    )
                }
            }
        }
    }
    val repoCountAndUpdated: @Composable (Dp) -> Unit = { startPadding ->
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.padding(start = startPadding),
        ) {
            Text(
                repoCountLabel(hiddenGroups.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        Text(
            uiState.lastUpdatedAtMs?.let { "Updated ${formatRelativeTime(it)}" }.orEmpty(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
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
            topBar = {
                TopAppBar(
                    title = {
                        // In landscape this content lives in the left rail instead.
                        if (!isLandscape) {
                            Column {
                                modePill()
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 4.dp),
                                ) {
                                    repoCountAndUpdated(0.dp)
                                }
                            }
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
                                // Always-visible app name and build label, so the running build can be checked against
                                // the version line in chat.
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    Text(
                                        "GitHubIssueFinder",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    VersionLabel(label = viewModel.buildLabel)
                                }
                                HorizontalDivider()
                                // Three groups, each with its own header; the dev-only Sandbox group is last.
                                DrawerMenuSection(header = "Repo Management", background = DrawerSectionRepoManagement) {
                                    DropdownMenuItem(
                                        text = { Text("Bookmarks") },
                                        onClick = {
                                            showMenu = false
                                            onOpenBookmarks()
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Hidden Issues") },
                                        onClick = {
                                            showMenu = false
                                            onOpenHidden()
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Check for New Repos") },
                                        onClick = {
                                            showMenu = false
                                            viewModel.checkForNewRepos()
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Clear Cache") },
                                        onClick = {
                                            showMenu = false
                                            showClearCacheConfirm = true
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Settings") },
                                        onClick = {
                                            showMenu = false
                                            onOpenSettings()
                                        },
                                    )
                                    // No confirmation: nothing is destroyed. Clears the mode and returns to the gate screen.
                                    DropdownMenuItem(
                                        text = { Text("Reset App") },
                                        onClick = {
                                            showMenu = false
                                            onResetApp()
                                        },
                                    )
                                }
                                HorizontalDivider()
                                DrawerMenuSection(header = "Find Issues", background = DrawerSectionFindIssues) {
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
                                HorizontalDivider()
                                DrawerMenuSection(header = "Sandbox", background = DrawerSectionSandbox) {
                                    // Detailed menu: one entry per animation (a dev aid). Otherwise a single "Play App Start Animation".
                                    if (viewModel.showDetailedDrawerMenu) {
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
                                        DropdownMenuItem(
                                            text = { Text("Play Title Reveal Animation (0.5x Speed)") },
                                            onClick = {
                                                showMenu = false
                                                replayAnimation = "titleRevealHalfSpeed"
                                            },
                                        )
                                    } else {
                                        DropdownMenuItem(
                                            text = { Text("Play App Start Animation") },
                                            onClick = {
                                                showMenu = false
                                                replayAnimation = "appStart"
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    },
                    actions = {
                        // Always shown (a placeholder with no data yet) so the title bar does not shift. With a token
                        // problem the tap explains it instead of opening the rate-limit dialog.
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
                        modePill()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 8.dp),
                        ) {
                            repoCountAndUpdated(0.dp)
                        }
                        LanguageFilterRow(
                            selected = uiState.selectedLanguage,
                            onSelect = viewModel::onLanguageSelected,
                            wrap = true,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                    RepoListBody(
                        uiState = uiState,
                        hiddenGroups = hiddenGroups,
                        currentMode = currentMode,
                        viewModel = viewModel,
                        onRepoClick = onRepoClick,
                        onOpenSettings = onOpenSettings,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            } else {
                Column(modifier = Modifier.padding(padding)) {
                    LanguageFilterRow(
                        selected = uiState.selectedLanguage,
                        onSelect = viewModel::onLanguageSelected,
                    )
                    RepoListBody(
                        uiState = uiState,
                        hiddenGroups = hiddenGroups,
                        currentMode = currentMode,
                        viewModel = viewModel,
                        onRepoClick = onRepoClick,
                        onOpenSettings = onOpenSettings,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        if (replayAnimation != null) {
            val onFinished = { replayAnimation = null }
            when (replayAnimation) {
                "origami" -> OrigamiSplashScreen(onFinished = onFinished)
                "shapeStack" -> ShapeStackSplashScreen(onFinished = onFinished)
                "titleReveal" -> TitleRevealSplashScreen(onFinished = onFinished)
                "titleRevealHalfSpeed" -> TitleRevealSplashScreen(onFinished = onFinished, speedMultiplier = 0.5f)
                "appStart" -> AppStartSplashScreen(onFinished = onFinished, animation = viewModel.splashAnimation)
                else -> SplashScreen(onFinished = onFinished)
            }
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
        AlertDialog(
            onDismissRequest = { showClearCacheConfirm = false },
            title = { Text("Clear cache?") },
            text = {
                Text(
                    "Removes every cached repo/issue list so the next load fetches live data. " +
                        "Bookmarks and hidden issues are kept.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showClearCacheConfirm = false
                    viewModel.clearCache()
                }) {
                    Text("Clear Cache")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheConfirm = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

/**
 * The list body: exactly one of no token, loading, error, empty, or the list with its
 * Load More footer. Shared by the portrait and landscape layouts.
 */
@Composable
private fun RepoListBody(
    uiState: TopReposUiState,
    hiddenGroups: List<RepoIssueGroup>,
    currentMode: IssueLabelFilter,
    viewModel: TopReposViewModel,
    onRepoClick: (RepoIssueGroup, LanguageFilter) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snippets by viewModel.snippets.collectAsStateWithLifecycle()
    val rawGroupsByName = remember(uiState.repoGroups) { uiState.repoGroups.associateBy { it.repoFullName } }
    Box(modifier = modifier) {
        when {
            uiState.noTokenConfigured -> NoTokenMessage(onOpenSettings = onOpenSettings)
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
            hiddenGroups.isEmpty() -> CenteredMessage(
                text = "No repos with an open ${currentMode.displayName} issue found for " +
                    "this language right now.",
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(hiddenGroups, key = { it.repoFullName }) { group ->
                    // The repo screen gets the raw group: it applies the hidden ids itself and needs every
                    // issue to show "all hidden" and to bring one back on Unhide.
                    RepoCard(
                        group = group,
                        snippet = snippets[group.repoFullName],
                        onShown = { viewModel.onRepoCardShown(group.repoOwner, group.repoName, group.repoFullName) },
                        onClick = { onRepoClick(rawGroupsByName[group.repoFullName] ?: group, uiState.selectedLanguage) },
                    )
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

/** How much bigger the rate-limit chip is than its original size (50% larger). */
private const val RATE_LIMIT_CHIP_SCALE = 1.5f

/**
 * Tappable rate-limit indicator: a meter icon and remaining/limit. Always rendered (a
 * placeholder when there are no numbers yet) so it does not pop in. Opens the dial dialog.
 */
@Composable
private fun RateLimitChip(info: RateLimitInfo?, onClick: () -> Unit) {
    val base = MaterialTheme.typography.bodySmall
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape((12 * RATE_LIMIT_CHIP_SCALE).dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.padding(end = (8 * RATE_LIMIT_CHIP_SCALE).dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                horizontal = (10 * RATE_LIMIT_CHIP_SCALE).dp,
                vertical = (4 * RATE_LIMIT_CHIP_SCALE).dp,
            ),
        ) {
            RateLimitMeterIcon()
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                // Normally supplied with the first list (a live search, or a /rate_limit lookup after a
                // cache hit); the placeholder shows only briefly or if that lookup fails.
                if (info != null) "${info.remaining}/${info.limit}" else "—",
                style = base.copy(
                    fontSize = base.fontSize * RATE_LIMIT_CHIP_SCALE,
                    lineHeight = base.lineHeight * RATE_LIMIT_CHIP_SCALE,
                ),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

/**
 * A small static speedometer glyph: red, amber and green segments, a needle and a hub.
 * It says "meter" and nothing about the value, which the numbers beside it give. Drawn with
 * Canvas because thick segments stay legible at this size where an emoji does not.
 */
@Composable
private fun RateLimitMeterIcon(modifier: Modifier = Modifier) {
    val needleColor = MaterialTheme.colorScheme.onSecondaryContainer
    Canvas(
        modifier = modifier
            .size(width = 24.dp, height = 15.dp)
            .semantics { contentDescription = "Search API rate limit" },
    ) {
        val strokeWidthPx = 4.dp.toPx()
        val hubRadiusPx = 2.5.dp.toPx()
        val radius = size.width / 2f - strokeWidthPx / 2f
        val center = Offset(size.width / 2f, size.height - hubRadiusPx)
        val arcTopLeft = Offset(center.x - radius, center.y - radius)
        val arcSize = Size(radius * 2f, radius * 2f)

        // Three 56-degree segments with 6-degree gaps across the top half.
        val segmentColors = listOf(Color(0xFFE53935), Color(0xFFFB8C00), Color(0xFF43A047))
        segmentColors.forEachIndexed { index, color ->
            drawArc(
                color = color,
                startAngle = 180f + index * 62f + 3f,
                sweepAngle = 56f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx),
            )
        }

        // A fixed needle, pointing up and a little to the right.
        val needleAngleRad = Math.toRadians(305.0)
        val needleTip = Offset(
            center.x + (radius - 1.dp.toPx()) * cos(needleAngleRad).toFloat(),
            center.y + (radius - 1.dp.toPx()) * sin(needleAngleRad).toFloat(),
        )
        drawLine(
            color = needleColor,
            start = center,
            end = needleTip,
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawCircle(color = needleColor, radius = hubRadiusPx, center = center)
    }
}

/** Text below the big number in the rate-limit dialog is 50% larger. */
private const val RATE_LIMIT_DIALOG_TEXT_SCALE = 1.5f

/** This style with its font size and line height scaled by [RATE_LIMIT_DIALOG_TEXT_SCALE]. */
private fun TextStyle.rateLimitDialogScaled(): TextStyle = copy(
    fontSize = fontSize * RATE_LIMIT_DIALOG_TEXT_SCALE,
    lineHeight = lineHeight * RATE_LIMIT_DIALOG_TEXT_SCALE,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RateLimitDialog(info: RateLimitInfo?, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = { Text("Search API Rate Limit") },
        text = {
            if (info == null) {
                Text("No rate limit data available yet.")
            } else {
                // Scrollable because the larger text can make the dialog
                // taller than the screen, e.g. in landscape.
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    RateLimitDial(info = info)
                    RateLimitRow("Remaining", "${info.remaining}")
                    RateLimitRow("Limit", "${info.limit}")
                    RateLimitRow("Used", "${info.used}")
                    RateLimitRow("Resets in", info.resetsInLabel)
                    Text(
                        "This reflects GitHub's Search API limit specifically, which is " +
                            "separate from and stricter than the general REST API limit.",
                        style = MaterialTheme.typography.bodySmall.rateLimitDialogScaled(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
        },
    )
}

/**
 * Hand-drawn semicircular dial: a fixed red-to-green track with a needle at the current
 * remaining/limit ratio. A needle, not a knob, because the gauge is display-only. The text
 * sits below the Canvas as ordinary Text, which lays out more simply than text in the arc.
 */
@Composable
private fun RateLimitDial(info: RateLimitInfo, modifier: Modifier = Modifier) {
    val ratio = (info.remaining.toFloat() / info.limit.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val needleColor = MaterialTheme.colorScheme.onSurface

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .semantics { contentDescription = "Rate limit gauge: ${info.remaining} of ${info.limit} remaining" },
        ) {
            val strokeWidthPx = 20.dp.toPx()
            val radius = (minOf(size.width / 2f, size.height) - strokeWidthPx / 2f).coerceAtLeast(0f)
            val center = Offset(size.width / 2f, size.height - strokeWidthPx / 2f)
            val arcTopLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2f, radius * 2f)

            // Fixed track: red at 180° (left, "0") through orange and yellow to green at 0° (right,
            // "max"). Only the visible half is drawn.
            val trackBrush = Brush.sweepGradient(
                0.0f to Color(0xFF43A047),
                0.5f to Color(0xFFE53935),
                0.667f to Color(0xFFFB8C00),
                0.833f to Color(0xFFFDD835),
                1.0f to Color(0xFF43A047),
                center = center,
            )

            drawArc(
                brush = trackBrush,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
            )

            // Pointer position: ratio=0 (empty) -> 180° (left/red).
            // ratio=1 (full) -> 360°/0° (right/green).
            val needleAngleRad = Math.toRadians((180f + 180f * ratio).toDouble())
            val needleTip = Offset(
                center.x + radius * cos(needleAngleRad).toFloat(),
                center.y + radius * sin(needleAngleRad).toFloat(),
            )

            drawLine(
                color = needleColor,
                start = center,
                end = needleTip,
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawCircle(color = needleColor, radius = 8.dp.toPx(), center = center)
        }

        Text("${info.remaining}", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Remaining",
            style = MaterialTheme.typography.bodySmall.rateLimitDialogScaled(),
            color = onSurfaceVariant,
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("0", style = MaterialTheme.typography.labelSmall.rateLimitDialogScaled(), color = onSurfaceVariant)
            Text(
                "${info.limit}",
                style = MaterialTheme.typography.labelSmall.rateLimitDialogScaled(),
                color = onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RateLimitRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium.rateLimitDialogScaled())
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.rateLimitDialogScaled(),
            fontWeight = FontWeight.Medium,
        )
    }
}

/**
 * The "no token" state: an explanation and a button to Settings. Saving a token reloads
 * the list on its own.
 */
@Composable
private fun NoTokenMessage(onOpenSettings: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "This screen needs a live GitHub connection. Add a free GitHub token " +
                    "in Settings to see ranked repos here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onOpenSettings, modifier = Modifier.padding(top = 12.dp)) {
                Text("Add your token")
            }
        }
    }
}

/**
 * Shown instead of RateLimitDialog on a token problem: none configured, or one GitHub
 * rejected (HTTP 401). Tapping the chip then is a setup problem, not "check back later",
 * so this explains the situation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TokenProblemDialog(
    tokenRejected: Boolean,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showHelp by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    onOpenSettings()
                },
            ) { Text("Open Settings") }
        },
        dismissButton = {
            TextButton(onClick = { showHelp = true }) { Text("How to get a token") }
        },
        title = { Text("GitHub API token needed") },
        text = {
            Text(
                if (tokenRejected) {
                    "GitHub isn't accepting the token this app is using (it may be " +
                        "expired, revoked, or otherwise invalid). Enter a new personal " +
                        "access token in Settings."
                } else {
                    "This app needs a GitHub personal access token to make live requests " +
                        "against the Search API. Enter one in Settings."
                },
            )
        },
    )
    if (showHelp) {
        ApiTokenHelpDialog(onDismiss = { showHelp = false })
    }
}

/** A pastel-tinted, headed group of menu items. Callers put a divider between sections. */
@Composable
private fun DrawerMenuSection(
    header: String,
    background: Color,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.background(background)) {
        Text(
            header,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 2.dp),
        )
        content()
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
 * A repo card: title, match-count badge and a README preview (2 lines, expandable with a
 * chevron). The chevron is its own Surface(onClick) so tapping it does not open the repo.
 * The preview renders nothing while loading or when there is no README, since a spinner or a
 * "no README" note would be clutter. Fetched by ReadmeSnippetRepository, requested by the
 * ViewModel when the card first appears.
 */
@Composable
private fun RepoCard(
    group: RepoIssueGroup,
    snippet: String?,
    onShown: () -> Unit,
    onClick: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(300),
        label = "repoCardSnippetChevron",
    )
    LaunchedEffect(group.repoFullName) {
        onShown()
    }

    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    group.repoDisplayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        if (group.issues.isEmpty()) "All hidden" else matchCountLabel(group.matchCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
            if (!snippet.isNullOrBlank()) {
                if (expanded) {
                    // Expanded: the chevron gets its own row under the full snippet.
                    Column(modifier = Modifier.padding(top = 6.dp)) {
                        Text(
                            snippet,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        ReadmeSnippetChevron(
                            rotation = chevronRotation,
                            onClick = { expanded = !expanded },
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                } else {
                    // Collapsed: the chevron sits inline at the end of the 2-line text to save a row. Compose
                    // Text cannot place it after only the last wrapped line, so both lines narrow slightly.
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            snippet,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        ReadmeSnippetChevron(
                            rotation = chevronRotation,
                            onClick = { expanded = !expanded },
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun matchCountLabel(count: Int): String = if (count == 1) "1 match" else "$count matches"

/**
 * The expand/collapse chevron for a card's README preview; used inline when collapsed and
 * on its own row when expanded.
 */
@Composable
private fun ReadmeSnippetChevron(rotation: Float, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        onClick = onClick,
        modifier = modifier,
    ) {
        Text(
            "⌄",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 1.dp)
                .rotate(rotation),
        )
    }
}

private fun repoCountLabel(count: Int): String = if (count == 1) "1 repo" else "$count repos"

/**
 * The pagination footer: Load More, a spinner while loading, a retry row on failure, or an
 * end-of-results line. Load More is a manual tap, not auto-load on scroll, so each Search API
 * call is an explicit action.
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

