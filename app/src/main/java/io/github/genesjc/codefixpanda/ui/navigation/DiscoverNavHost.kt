package io.github.genesjc.codefixpanda.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.genesjc.codefixpanda.ui.bookmarks.RepoBookmarksScreen
import io.github.genesjc.codefixpanda.ui.hidden.HiddenIssuesScreen
import io.github.genesjc.codefixpanda.ui.modegate.IssueModeGateScreen
import io.github.genesjc.codefixpanda.ui.modegate.IssueModeViewModel
import io.github.genesjc.codefixpanda.ui.repoissues.RepoIssuesScreen
import io.github.genesjc.codefixpanda.ui.settings.SettingsScreen
import io.github.genesjc.codefixpanda.ui.toprepos.TopReposScreen
import io.github.genesjc.codefixpanda.ui.welcome.TokenGuideScreen
import io.github.genesjc.codefixpanda.ui.welcome.WelcomeScreen
import io.github.genesjc.codefixpanda.ui.welcome.WelcomeViewModel

private const val ROUTE_WELCOME = "welcome"
private const val ROUTE_TOKEN_GUIDE = "tokenGuide"
private const val ROUTE_ISSUE_MODE_GATE = "issueModeGate"
private const val ROUTE_TOP_REPOS = "topRepos"
private const val ROUTE_REPO_ISSUES = "repoIssues"
private const val ROUTE_REPO_BOOKMARKS = "repoBookmarks"
private const val ROUTE_HIDDEN_ISSUES = "hiddenIssues"
private const val ROUTE_SETTINGS = "settings"

/**
 * Double-tapping Back can fire two pops before the first transition removes the button; the
 * second lands on an entry already leaving and leaves a blank screen. Only the RESUMED entry
 * may pop.
 */
private fun NavController.popBackStackDebounced() {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        popBackStack()
    }
}

/**
 * Navigation for the Discover flow: the welcome, the mode gate, Top Repos, a repo's issues, bookmarks,
 * hidden issues and settings, on Navigation Compose for real system Back support.
 * The gate is a start destination inside this NavHost, not an early return outside it: outside, Top
 * Repos would be the only back-stack entry and Back would close the app. The start destination is
 * computed once: the gate if no mode is chosen yet, otherwise Top Repos.
 * The selected repo group and language are held by RepoSelectionViewModel, not passed through the
 * route; if the back stack is restored without them (the process was killed), the repo screen's route
 * returns to Top Repos. "Reset App" clears the mode and pops the whole back stack, so the gate is not
 * duplicated. More in _meta/docs/design-notes.md, "Navigation".
 */
@Composable
fun DiscoverNavHost(
    modifier: Modifier = Modifier,
    issueModeViewModel: IssueModeViewModel = viewModel(),
    welcomeViewModel: WelcomeViewModel = viewModel(),
    selectionViewModel: RepoSelectionViewModel = viewModel(),
) {
    val navController = rememberNavController()
    val selection by selectionViewModel.selection.collectAsStateWithLifecycle()

    // The welcome comes first for someone with no token who has not seen it; then the gate if no mode is
    // chosen; otherwise Top Repos.
    val startDestination = remember {
        when {
            welcomeViewModel.needsWelcomeAtStart() -> ROUTE_WELCOME
            issueModeViewModel.isGateNeeded() -> ROUTE_ISSUE_MODE_GATE
            else -> ROUTE_TOP_REPOS
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(ROUTE_WELCOME) {
            val next = if (issueModeViewModel.isGateNeeded()) ROUTE_ISSUE_MODE_GATE else ROUTE_TOP_REPOS

            WelcomeScreen(
                onOpenGuide = { navController.navigate(ROUTE_TOKEN_GUIDE) },
                onTokenSaved = {
                    if (navController.previousBackStackEntry != null) {
                        // Reopened from the menu: back to where the user was, which now shows the new token's data.
                        navController.popBackStackDebounced()
                    } else {
                        // Shown at start: on to the gate or Top Repos, with nothing to come back to.
                        navController.navigate(next) { popUpTo(ROUTE_WELCOME) { inclusive = true } }
                    }
                },
                // The demo is always a step forward, whichever way the welcome was reached, so Back returns to it.
                onTryDemo = { navController.navigate(next) { launchSingleTop = true } },
            )
        }

        composable(ROUTE_TOKEN_GUIDE) {
            TokenGuideScreen(onBack = { navController.popBackStackDebounced() })
        }

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
                    selectionViewModel.select(group, language)
                    navController.navigate(ROUTE_REPO_ISSUES)
                },
                onOpenBookmarks = { navController.navigate(ROUTE_REPO_BOOKMARKS) },
                onOpenHidden = { navController.navigate(ROUTE_HIDDEN_ISSUES) },
                onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
                onOpenWelcome = { navController.navigate(ROUTE_WELCOME) },
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
            val current = selection
            if (current != null) {
                RepoIssuesScreen(
                    repoGroup = current.repoGroup,
                    language = current.language,
                    onBack = { navController.popBackStackDebounced() },
                    onOpenHidden = { navController.navigate(ROUTE_HIDDEN_ISSUES) },
                )
            } else {
                // The back stack was restored after the process was killed, and the choice of repo was lost with
                // it: go back to Top Repos rather than showing a blank screen.
                LaunchedEffect(Unit) { navController.popBackStack() }
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
