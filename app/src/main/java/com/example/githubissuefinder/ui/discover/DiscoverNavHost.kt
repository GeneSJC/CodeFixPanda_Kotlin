package com.example.githubissuefinder.ui.discover

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.githubissuefinder.data.needsModeGate
import com.example.githubissuefinder.model.LanguageFilter
import com.example.githubissuefinder.model.RepoIssueGroup
import com.example.githubissuefinder.ui.settings.SettingsScreen
import org.koin.compose.viewmodel.koinViewModel

private const val ROUTE_ISSUE_MODE_GATE = "issueModeGate"
private const val ROUTE_TOP_REPOS = "topRepos"
private const val ROUTE_REPO_ISSUES = "repoIssues"
private const val ROUTE_REPO_BOOKMARKS = "repoBookmarks"
private const val ROUTE_HIDDEN_ISSUES = "hiddenIssues"
private const val ROUTE_SETTINGS = "settings"

/**
 * Navigation host for the "repoFirst" Discover flow — introduced in
 * v0.38 alongside RepoIssuesScreen, the drill-in every Discover CM
 * since v0.25 had flagged as deferred (tapping a repo was a no-op).
 *
 * Uses Navigation Compose specifically for real
 * system back-button support, unlike the simple boolean-state-swap
 * pattern used for the splash-replay overlay (v0.31) — that was a
 * short, self-contained overlay; this is a genuine set of screens a
 * user will expect to back out of properly.
 *
 * The selected repo group is held as plain Compose state at THIS
 * level, NOT serialized through the nav route itself — avoids needing
 * Parcelable/Serializable ceremony on the model just to pass it through
 * a nav argument, since the full object is already in memory from the
 * list TopReposScreen loaded.
 *
 * v0.42: holds a RepoIssueGroup instead of a Repo, following
 * TopReposScreen's data-layer swap — see that screen and
 * TopReposStateHolder's class comment. Nothing about this file's own
 * navigation STRUCTURE changed for that; only the type flowing
 * through it did.
 *
 * The gate (ROUTE_ISSUE_MODE_GATE) is a real destination inside this
 * same NavHost, reached as the actual startDestination — NOT an early
 * `return` outside the NavHost entirely. An earlier version of this
 * screen (during v0.41) used that early-return shape and it broke
 * system back navigation: with the gate living outside the NavHost,
 * Top Repos was the only thing ever pushed onto its back stack, so
 * pressing back from it had nothing to pop, and the press propagated
 * past this composable and closed the app instead of returning to the
 * gate. That was only found by actually pressing back on a running
 * device, then fixed directly in v0.41 before this v0.42 file was
 * built on top of it — this file carries that same real-destination
 * structure forward rather than reintroducing the early-return shape.
 *
 * startDestination is computed ONCE, via `remember` (NOT re-read
 * reactively on every Issue Mode change — NavHost's
 * startDestination is fixed at creation) — gate if no mode is chosen
 * yet, straight to Top Repos if one already is. As of v0.44, that
 * second branch is genuinely reachable, not just forward-compatible —
 * Mode now persists across launches (see IssueModeRepository.kt), so a returning
 * user's saved mode is already restored by the time this composes, and
 * they land directly on Top Repos with the gate skipped entirely.
 * Picking a mode on the gate screen calls
 * navController.navigate(ROUTE_TOP_REPOS), pushing Top Repos ON TOP of
 * the gate rather than replacing it, so the gate stays in the back
 * stack underneath and system back from Top Repos correctly returns
 * to it — but ONLY within a session that actually visited the gate.
 *
 * v0.48: added ROUTE_REPO_BOOKMARKS, reached from TopReposScreen's
 * hamburger menu ("Bookmarks") — pushed on top of Top Repos, same
 * "push, don't replace" pattern every other forward navigation in this
 * file already uses, so back from it returns to wherever it was opened
 * from.
 *
 * v0.51 ("Hide Issue"): added ROUTE_HIDDEN_ISSUES, reachable from BOTH
 * TopReposScreen's hamburger menu ("Hidden") and RepoIssuesScreen's
 * all-hidden empty state ("View Hidden") — same push-on-top pattern,
 * so back returns to whichever of the two it was opened from.
 *
 * A ROUTE_REPO_README destination (a separate full-screen README
 * viewer, reached from a "📄" toolbar action) briefly existed here and
 * was removed once RepoIssuesScreen gained its own inline, expandable
 * README widget (ReadmeExpandableSection) — the originally-discussed
 * design, built once the card-density pass it was deliberately
 * deferred behind had landed. A separate nav destination for the same
 * content would have been a redundant second access point, not a
 * reasonable fallback, so it came out rather than staying alongside
 * the new widget.
 *
 * "Reset App" (TopReposScreen's hamburger menu): the gate
 * (ROUTE_ISSUE_MODE_GATE) is otherwise genuinely unreachable again once
 * a mode is chosen — picking a mode only ever pushes Top Repos on top
 * of it, and the two in-place mode-switch menu items change
 * the mode without touching navigation at all, so there was no
 * way back to the gate short of clearing app data. Reset App clears
 * the mode (IssueModeViewModel.clearMode()) and navigates to ROUTE_ISSUE_MODE_GATE with
 * popUpTo(graph.id) { inclusive = true }, wiping the ENTIRE back stack
 * (not just popping once) — a plain navigate() would instead push a
 * second, duplicate gate entry on top of whatever screens were open,
 * leaving them reachable again via back. Bookmarks, hidden issues, and
 * the repo cache are deliberately untouched — only IssueMode and
 * navigation position reset.
 */
/**
 * Rapidly double-tapping a back IconButton can fire onClick twice before
 * the first pop's transition removes the button from composition, so a
 * second popBackStack() lands on a back-stack entry that's already
 * mid-way out — one of those two pops has nothing valid left to land on,
 * which is what left a blank screen after popping RepoIssues -> Top
 * Repos -> (nothing). Only the entry that is actually RESUMED (i.e. the
 * real current top, not one already animating away) is allowed to pop.
 */
private fun NavController.popBackStackDebounced() {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        popBackStack()
    }
}

@Composable
fun DiscoverNavHost(
    modifier: Modifier = Modifier,
    issueModeViewModel: IssueModeViewModel = koinViewModel(),
) {
    val navController = rememberNavController()
    var selectedRepoGroup by remember { mutableStateOf<RepoIssueGroup?>(null) }
    // v0.50: the LanguageFilter Top Repos was actually showing when the
    // tapped repo's card was built — needed by RepoIssuesScreen's
    // "Refresh this repo" write-back to address the correct
    // (language, mode) cache entry. Held alongside selectedRepoGroup,
    // same "plain Compose state at THIS level, not through the nav
    // route" pattern that group already uses.
    var selectedLanguage by remember { mutableStateOf(LanguageFilter.KOTLIN) }

    val startDestination = remember {
        if (needsModeGate(issueModeViewModel.mode.value)) ROUTE_ISSUE_MODE_GATE else ROUTE_TOP_REPOS
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(ROUTE_ISSUE_MODE_GATE) {
            IssueModeGateScreen(
                onModeSelected = { mode ->
                    issueModeViewModel.selectMode(mode)
                    navController.navigate(ROUTE_TOP_REPOS)
                },
            )
        }
        composable(ROUTE_TOP_REPOS) {
            TopReposScreen(
                onRepoClick = { group, language ->
                    selectedRepoGroup = group
                    selectedLanguage = language
                    navController.navigate(ROUTE_REPO_ISSUES)
                },
                onOpenBookmarks = { navController.navigate(ROUTE_REPO_BOOKMARKS) },
                onOpenHidden = { navController.navigate(ROUTE_HIDDEN_ISSUES) },
                onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
                onResetApp = {
                    issueModeViewModel.clearMode()
                    navController.navigate(ROUTE_ISSUE_MODE_GATE) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(ROUTE_REPO_ISSUES) {
            selectedRepoGroup?.let { group ->
                RepoIssuesScreen(
                    repoGroup = group,
                    language = selectedLanguage,
                    onBack = { navController.popBackStackDebounced() },
                    onOpenHidden = { navController.navigate(ROUTE_HIDDEN_ISSUES) },
                )
            }
        }
        composable(ROUTE_REPO_BOOKMARKS) {
            RepoBookmarksScreen(onBack = { navController.popBackStackDebounced() })
        }
        composable(ROUTE_HIDDEN_ISSUES) {
            HiddenIssuesScreen(onBack = { navController.popBackStackDebounced() })
        }
        composable(ROUTE_SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStackDebounced() })
        }
    }
}
