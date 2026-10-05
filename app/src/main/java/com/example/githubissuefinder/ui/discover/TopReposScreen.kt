package com.example.githubissuefinder.ui.discover

import com.example.githubissuefinder.BuildConfig

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.githubissuefinder.data.HiddenIssuesStore
import com.example.githubissuefinder.data.RateLimitInfo
import com.example.githubissuefinder.data.RateLimitState
import com.example.githubissuefinder.data.filterHiddenIssues
import com.example.githubissuefinder.data.formatRelativeTime
import com.example.githubissuefinder.model.IssueLabelFilter
import com.example.githubissuefinder.model.LanguageFilter
import com.example.githubissuefinder.model.RepoIssueGroup
import com.example.githubissuefinder.network.GitHubApiClient
import com.example.githubissuefinder.ui.components.LabelInfoDialog
import com.example.githubissuefinder.ui.components.LanguageFilterRow
import com.example.githubissuefinder.ui.components.VersionLabel
import com.example.githubissuefinder.ui.theme.colorScheme
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
 * CM 1 of the "Discover" repo-first flow (option C from the architecture
 * discussion).
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
 * The hamburger menu (☰, v0.31, expanded over time as more animations
 * were added) opens with an OVERLAY-based animation-replay toolkit —
 * replayAnimation is local state inside THIS function, so viewModel()
 * above is only ever called once per real session — but what it shows
 * now depends on BuildConfig.SHOW_DETAILED_DRAWER_MENU (a plain
 * boolean val in app/build.gradle.kts, defaulting to false): true gets
 * everything that existed before this CM, plus one entry per
 * individual splash: "Play Origami Animation" / "Play Shape Stack
 * Animation" / "Play Coin Animation" / "Play Title Reveal Animation" /
 * "Play Title Reveal Animation (0.5x Speed)" — the two Title Reveal
 * entries are the SAME composable called with a different
 * speedMultiplier, not two separate screens, see
 * TitleRevealSplashScreen.kt's doc comment). false (the default) drops
 * the version stamp and all five individual entries in favor of one:
 * "Play App Start Animation", which replays AppStartSplashScreen.kt —
 * the same dispatch MainActivity itself uses at real cold start, so it
 * can never show something an actual user wouldn't have actually seen.
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
 * the animation items via their own divider: "Check for New Repos"
 * (renamed in v0.49 from "Refresh" — was a forced live fetch that
 * REPLACED the whole list with just its own results, silently
 * discarding whatever Load More had accumulated; now merges fresh
 * results in instead, see TopReposViewModel.checkForNewRepos()'s doc
 * comment) and "Clear Cache" (wipes the ENTIRE persistent cache, not
 * just the active combination — see RepoIssueGroupCache.clearAll()'s
 * doc comment for why that's a known, named limitation rather than an
 * oversight — and, as of v0.49, reloads this screen immediately
 * afterward rather than leaving stale data visible).
 *
 * v0.46: a repo-count chip shows how many repo cards are currently
 * loaded, styled directly after the classic flow's own RateLimitChip,
 * the same reference point this was asked for by name. Just reads
 * uiState.repoGroups.size directly; no separate tracking needed,
 * updates for free on every Load More and every mode/language switch.
 * Originally lived in the top bar's actions slot; moved down to the
 * "Updated X ago" row (right-aligned, opposite that text) once the
 * ported RateLimitChip itself took over that actions slot — see the
 * newer paragraph below for the full reasoning.
 *
 * v0.48: added "Bookmarks" as the very first menu item, right after
 * the version header — a real navigation destination (see
 * DiscoverNavHost's new ROUTE_REPO_BOOKMARKS and RepoBookmarksScreen),
 * placed ahead of the animation/settings-style items below it since
 * it's the one item in this menu someone might actually want to reach
 * repeatedly, not a one-off action.
 *
 * v0.51 ("Hide Issue", Parts C+D of the Cache-Preserving Refresh
 * sequence): uiState.repoGroups stays the RAW cached data — hiddenGroups
 * below is a display-only derivation (each group's issues filtered
 * against HiddenIssuesStore.hiddenIds, empty groups dropped entirely)
 * computed fresh on every recomposition, never written back anywhere.
 * The repo count chip and every match-count badge read from
 * hiddenGroups, not uiState.repoGroups directly, so hiding an issue
 * down to a repo's last match makes that whole card disappear, and
 * unhiding brings it straight back — falls out of Compose recomposition
 * automatically since HiddenIssuesStore.hiddenIds is itself
 * Compose-reactive (mutableStateOf), same mechanism RepoBookmarks
 * already relies on elsewhere in this flow.
 *
 * Also added: a "Hidden Issues" drawer item (next to "Bookmarks", same
 * always-shown placement regardless of SHOW_DETAILED_DRAWER_MENU), and
 * a confirmation dialog in front of "Clear Cache" (Part D) — this menu
 * previously fired viewModel.clearCache() directly with no
 * confirmation at all; the dialog's text is explicit that bookmarks
 * and hidden issues are UNaffected, since clearCache() only ever
 * touches RepoIssueGroupCache, never RepoBookmarks or
 * HiddenIssuesStore.
 *
 * Reorganized into three pastel-tinted, headed groups (DrawerMenuSection,
 * below) after direct feedback that a long flat list was hard to scan:
 * "Repo Management" (Bookmarks, Hidden Issues, Check for New Repos,
 * Clear Cache — everything that acts on this screen's own data),
 * "Find Issues" (the two mode-switch items), and "Sandbox" (the CM
 * version stamp and animation-replay tooling, deliberately LAST since
 * it's dev-only and the least likely thing an end user needs). Also
 * renamed "Hidden" to "Hidden Issues" for the same reason. Each splash
 * screen (SplashScreen, OrigamiSplashScreen, ShapeStackSplashScreen,
 * TitleRevealSplashScreen) is now pinned to the Good First Issue
 * (purple) palette regardless of the app's active Issue Mode, so
 * replaying one from the Sandbox group — or a real cold start with Help
 * Wanted already chosen — no longer repaints it green; see
 * TitleRevealSplashScreen's own doc comment for the full reasoning.
 *
 * Ported the classic flow's rate-limit chip/dialog/dial (RateLimitChip
 * below) into this screen's TopAppBar actions — see RateLimitChip's own
 * doc comment for why it ALWAYS renders (a placeholder "🔑 —" when
 * RateLimitState.info is still null) rather than conditionally
 * appearing only once a live search completes: a cache hit never calls
 * the network at all, so on a typical revisit the chip could otherwise
 * stay absent for the entire session, and its sudden appearance
 * mid-session was colliding with the "good first issue" info icon
 * right next to it in the title. Taking over the actions slot pushed
 * the repo-count chip that used to live there down to its own row,
 * right-aligned opposite "Updated X ago" — a natural fit, since both
 * are specific to the active language filter rather than being
 * Mode-level chrome like the title bar itself.
 *
 * The title's mode label (currentMode.displayName, plus its "ⓘ" for
 * Good First Issue) now renders inside a colored pill — same
 * background-pill convention RepoIssuesScreen's own active-mode badge
 * uses (Modifier.background(mode.colorScheme().primary,
 * RoundedCornerShape(8.dp)), white labelLarge text), rather than plain
 * TopAppBar title text. Also: RepoCard's repoFullName subtitle is gone
 * (see that composable's own doc comment).
 *
 * The static "Repos" caption (v0.44, below) and the "Updated X ago" +
 * repo-count row (v0.45/v0.46, originally its own Row right under
 * LanguageFilterRow) are CONSOLIDATED into one: that whole second row
 * is gone from the content area, and its two pieces (the repo-count
 * chip, "Updated X ago") now render together on the line directly
 * under the mode pill, where "Repos" used to be alone — clearing a
 * full row of vertical space from the content layout, per direct
 * instruction.
 *
 * Landscape Compact Header: the first concrete step of a direct
 * request to improve landscape layout, after seeing this screen
 * running rotated — a landscape phone screen is short on height, not
 * width, and the title's own two text rows (mode pill; repo-count
 * chip + "Updated X ago") were eating a disproportionate share of
 * that scarce vertical space. isLandscape reads
 * LocalConfiguration.current.orientation directly (no ViewModel
 * involvement — purely a rendering choice, nothing about the
 * underlying data changes) and merges those two rows into one when
 * true.
 *
 * Landscape Split View: the larger step discussed alongside the
 * compact header above, now built. In landscape, the whole screen
 * splits into two columns instead of stacking everything in one: a
 * narrow (180dp), independently vertically-scrolling LEFT column
 * holding everything that otherwise sits ABOVE the card list — the
 * mode pill, repo-count/updated (modePill/repoCountAndUpdated, both
 * hoisted to this function's own scope so the TopAppBar's title slot
 * AND this left column can both render them, without duplicating
 * either), and LanguageFilterRow — and a RIGHT column (RepoListBody,
 * extracted below so it isn't duplicated between this layout and
 * portrait's) that's nothing but the card list, filling the screen's
 * full height. This is what actually delivers the original ask: the
 * card list gets every pixel of vertical space landscape has to give
 * it, rather than losing a large chunk of it to stacked header
 * content above.
 *
 * LanguageFilterRow is passed `wrap = true` here specifically — this
 * column is narrow, so the row's chips need multiple lines rather than
 * the portrait default of one scrolling line. A second horizontally-
 * scrolling row nested inside an already vertically-scrolling column
 * was deliberately ruled out (two competing scroll gestures in the
 * same small area), and wrapping also means every language option
 * stays visible at once rather than some being scrolled off-screen
 * with no visual hint they're there. See LanguageFilterRow's own doc
 * comment for the `wrap` parameter itself.
 *
 * Portrait is entirely unchanged: the TopAppBar's title renders the
 * mode pill + repo-count row as it always has, LanguageFilterRow stays
 * its original single scrolling line, and RepoListBody sits in a plain
 * Column beneath it — the exact same composable, just placed
 * differently, which is the whole reason RepoListBody exists as its
 * own function rather than being written out twice.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopReposScreen(
    onRepoClick: (RepoIssueGroup, LanguageFilter) -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHidden: () -> Unit,
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
    val rateLimit by RateLimitState.info.collectAsStateWithLifecycle()
    val hiddenGroups = filterHiddenIssues(uiState.repoGroups, HiddenIssuesStore.hiddenIds)
    // Guaranteed non-null: DiscoverNavHost never renders this screen
    // while IssueMode.current is null (see its own doc comment).
    val currentMode = IssueMode.current ?: IssueLabelFilter.GOOD_FIRST_ISSUE
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Hoisted to function scope (not local to the TopAppBar's title
    // slot) so the landscape split-view body below can render this
    // same content too — see "Landscape Split View" further down in
    // this doc comment.
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
                // "good first issue" reads less clearly on its own than
                // "help wanted" does, per direct feedback after seeing
                // this screen running — an info icon here explains what
                // it means. "help wanted" doesn't get one: it wasn't
                // reported as confusing, and the shared LabelInfoDialog
                // explaining both terms is still reachable from
                // RepoIssuesScreen for anyone who wants it there too.
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

    // v0.50: re-reads the cache on every fresh composition of this
    // screen — including, deliberately, when returning from
    // RepoIssuesScreen (which is exactly when it matters: a "Refresh
    // this repo" there writes straight to the cache, not to this
    // screen's own ViewModel, since the two have no direct handle to
    // each other — see TopReposViewModel.syncFromCache()'s own doc
    // comment). Also fires on this screen's very first appearance,
    // which is a harmless, cheap (local file, no network) re-read of
    // what init{}'s own load() just populated.
    LaunchedEffect(Unit) {
        viewModel.syncFromCache()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        // In landscape, this content moved into the split
                        // view's left column below (see "Landscape Split
                        // View" in this file's top doc comment) — the
                        // TopAppBar title renders nothing there, rather
                        // than showing it twice. Portrait is unchanged:
                        // the mode pill, then the repo-count chip +
                        // "Updated X ago" on the row beneath it.
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
                                // Always-visible header (Project Conventions
                                // doc, section 6): app name + VersionLabel
                                // ("Build A.B"), so the build actually running
                                // on a device can be checked against the
                                // version line a chat message ends with — NOT
                                // gated behind SHOW_DETAILED_DRAWER_MENU the
                                // way the old CM version stamp was, since this
                                // needs to be checkable regardless of that
                                // dev-only setting.
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    Text(
                                        "GitHubIssueFinder",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    VersionLabel()
                                }
                                HorizontalDivider()
                                // Three groups — Repo Management, Find Issues,
                                // Sandbox — each its own pastel-tinted section
                                // with its own header, separated by dividers.
                                // "Sandbox" (dev-only tooling) is deliberately
                                // last: it's the group an end user is least
                                // likely to need, not the first thing this
                                // menu shows.
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
                                    // true keeps every individual animation-replay
                                    // entry, useful for exactly the kind of
                                    // iteration this project does. false (the
                                    // default) strips that down to a single "Play
                                    // App Start Animation" entry, what an actual
                                    // end user would see — see app/build.gradle.kts's
                                    // showDetailedDrawerMenu val. The version stamp
                                    // this section used to show here (gated behind
                                    // the same flag) moved to the always-visible
                                    // header above, see VersionLabel.
                                    if (BuildConfig.SHOW_DETAILED_DRAWER_MENU) {
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
                        // Always shown, even with no data yet (a placeholder
                        // "🔑 —") rather than appearing/disappearing mid-
                        // session — a chip popping in once the first live
                        // search completes was visually colliding with the
                        // "good first issue" info icon in the title next to
                        // it, since the title bar had no stable reserved
                        // space for it. Tapping it opens RateLimitDialog
                        // normally, EXCEPT when there's a token problem —
                        // none configured at all (GitHubApiClient.hasToken
                        // false) or one configured but rejected by GitHub
                        // (uiState.tokenRejected, a 401 response) — in
                        // either case "No rate limit data available yet."
                        // would be misleading (it reads like "wait a
                        // moment," not "this needs attention"), so
                        // TokenProblemDialog explains the actual situation
                        // instead. Reaching that dialog with hasToken true
                        // can only mean tokenRejected is what triggered it,
                        // so hasToken itself is enough to pick the right
                        // message — no need to also thread tokenRejected
                        // through as a separate parameter.
                        RateLimitChip(
                            info = rateLimit,
                            onClick = {
                                if (GitHubApiClient.hasToken && !uiState.tokenRejected) {
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
                // Landscape split view: a narrow, independently-scrolling
                // left column holding everything that used to sit ABOVE
                // the card list (mode pill, repo-count/updated, language
                // picker) — freeing the right column to be nothing but
                // the card list, at the screen's full height. See
                // "Landscape Split View" in this file's top doc comment.
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
                "appStart" -> AppStartSplashScreen(onFinished = onFinished)
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
            tokenRejected = GitHubApiClient.hasToken,
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
 * The repo list's main body — one of four states (no token, loading,
 * error, empty, or the actual list + Load More footer), exactly one
 * shown at a time. Extracted out of TopReposScreen itself once the
 * landscape split view needed to render this SAME body in a
 * differently-shaped parent (a weighted column beside the left rail,
 * rather than stacked under LanguageFilterRow in a plain Column) —
 * rather than duplicate this whole `when` block for both layouts,
 * both call sites pass their own `modifier` (fillMaxSize either way,
 * but constrained differently by their respective parents) and get
 * identical content.
 */
@Composable
private fun RepoListBody(
    uiState: TopReposUiState,
    hiddenGroups: List<RepoIssueGroup>,
    currentMode: IssueLabelFilter,
    viewModel: TopReposViewModel,
    onRepoClick: (RepoIssueGroup, LanguageFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
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
                    RepoCard(group = group, onClick = { onRepoClick(group, uiState.selectedLanguage) })
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

/**
 * Persistent, tappable rate-limit indicator for the repo-first flow —
 * ported over from the classic flow's own FeedTabsScreen (see git
 * history), which had this same chip/dialog/dial but was removed along
 * with the rest of classic mode. repoFirst's data layer (RateLimitState,
 * fed by every successful TopReposViewModel/RepoIssuesViewModel search
 * response) already tracked this the whole time — this flow just never
 * displayed it anywhere, a known gap named in this project's own
 * RepoFirst_FeatureSet report. Added to TopReposScreen only for now;
 * RepoIssuesScreen is a reasonable follow-up.
 *
 * "🔑 remaining/limit" in a compact chip — there genuinely isn't room
 * for a legible dial in the TopAppBar itself. Tapping it opens a dialog
 * with the full speedometer-style dial below.
 */
@Composable
private fun RateLimitChip(info: RateLimitInfo?, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.padding(end = 8.dp),
    ) {
        Text(
            // A cache-hit load never calls the network, so info can stay
            // null for the whole session — "🔑 —" until a real search
            // response arrives, rather than the chip appearing/
            // disappearing depending on whether this exact combination
            // happened to be cached. Still tappable either way: the
            // dialog handles a null info with its own explanatory text.
            if (info != null) "🔑 ${info.remaining}/${info.limit}" else "🔑 —",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RateLimitDialog(info: RateLimitInfo?, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = { Text("Search API rate limit") },
        text = {
            if (info == null) {
                Text("No rate limit data available yet.")
            } else {
                Column {
                    RateLimitDial(info = info)
                    RateLimitRow("Remaining", "${info.remaining}")
                    RateLimitRow("Limit", "${info.limit}")
                    RateLimitRow("Used", "${info.used}")
                    RateLimitRow("Resets in", info.resetsInLabel)
                    Text(
                        "This reflects GitHub's Search API limit specifically, which is " +
                            "separate from and stricter than the general REST API limit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
        },
    )
}

/**
 * Hand-drawn semicircular dial: a FIXED red(left)-to-green(right) track,
 * evenly stepped through orange and yellow — a classic speedometer
 * red-line convention, independent of the current value. A separate
 * pointer dot marks where the current remaining/limit ratio actually
 * falls along that fixed track. Text ("29 Remaining") is rendered as
 * ordinary Compose Text below the Canvas, rather than drawn inside the
 * arc via Canvas.drawText — simpler and safer to lay out correctly than
 * measuring/centering native text inside a curved region.
 */
@Composable
private fun RateLimitDial(info: RateLimitInfo, modifier: Modifier = Modifier) {
    val ratio = (info.remaining.toFloat() / info.limit.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val dialogBackground = MaterialTheme.colorScheme.surface

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
            val strokeWidthPx = 20.dp.toPx()
            val radius = (minOf(size.width / 2f, size.height) - strokeWidthPx / 2f).coerceAtLeast(0f)
            val center = Offset(size.width / 2f, size.height - strokeWidthPx / 2f)
            val arcTopLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2f, radius * 2f)

            // Fixed track: red at 180° (left/"0" end) through orange and
            // yellow, to green at 360°/0° (right/"max" end) — the visible
            // half of a full sweepGradient. The 0.0-0.5 stops (the
            // invisible bottom-right-to-left half) are never drawn by the
            // drawArc call below, so their exact color doesn't matter.
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
            val pointerAngleRad = Math.toRadians((180f + 180f * ratio).toDouble())
            val pointerX = center.x + radius * cos(pointerAngleRad).toFloat()
            val pointerY = center.y + radius * sin(pointerAngleRad).toFloat()
            val pointerRadius = strokeWidthPx / 2f + 4.dp.toPx()

            drawCircle(color = dialogBackground, radius = pointerRadius, center = Offset(pointerX, pointerY))
            drawCircle(
                color = Color.Black.copy(alpha = 0.3f),
                radius = pointerRadius,
                center = Offset(pointerX, pointerY),
                style = Stroke(width = 2.dp.toPx()),
            )
        }

        Text("${info.remaining}", style = MaterialTheme.typography.headlineMedium)
        Text("Remaining", style = MaterialTheme.typography.bodySmall, color = onSurfaceVariant)

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("0", style = MaterialTheme.typography.labelSmall, color = onSurfaceVariant)
            Text("${info.limit}", style = MaterialTheme.typography.labelSmall, color = onSurfaceVariant)
        }
    }
}

@Composable
private fun RateLimitRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

/**
 * Shown instead of RateLimitDialog when there's a token problem —
 * either GitHubApiClient.hasToken is false (nothing configured at all)
 * or a configured token is being rejected by GitHub (tokenRejected,
 * true only once a live search has actually come back with an HTTP 401
 * — GitHub's own signal for bad/expired/revoked credentials, distinct
 * from a 403 rate-limit response). Either way, tapping the rate-limit
 * chip is a setup problem, not a "check back in a moment" one, so this
 * gets its own dialog with the actual situation rather than reusing
 * RateLimitDialog's generic "No rate limit data available yet." text,
 * which would leave someone in either case wondering what to even do
 * about it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TokenProblemDialog(tokenRejected: Boolean, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Got it") }
        },
        title = { Text("GitHub API token needed") },
        text = {
            Text(
                if (tokenRejected) {
                    "We see a value set for GITHUB_API_TOKEN in local.properties, but " +
                        "GitHub isn't accepting it (expired, revoked, or otherwise " +
                        "invalid). Generate a new personal access token and update " +
                        "local.properties, then rebuild."
                } else {
                    "This app needs a GitHub personal access token to make live requests " +
                        "against the Search API. Add one as GITHUB_API_TOKEN in your " +
                        "local.properties file (gitignored, never committed), then rebuild."
                },
            )
        },
    )
}

/**
 * One visually-grouped section of the hamburger menu's DropdownMenu — a
 * pastel-tinted background, a small bold header, then its items. Each
 * group is what "before there is a divider to the next group" means:
 * callers place a HorizontalDivider() between sections, not within one.
 */
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
 * v0.42: rewritten from the ground up for RepoIssueGroup instead of
 * Repo. Deliberately simple compared to the old card — title and a
 * match-count badge is genuinely all there is to show now (see this
 * file's top doc comment for what's gone and why).
 *
 * The repoFullName (owner/repo path) subtitle that used to sit under
 * the title is gone, same reasoning as IssueCard's own identity-row
 * removal: a screen already scoped to listing repos doesn't need each
 * card repeating a raw path alongside its own humanized title.
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

