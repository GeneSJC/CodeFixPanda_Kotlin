package io.github.genesjc.githubissuefinder.ui.discover

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.genesjc.githubissuefinder.data.needsModeGate
import io.github.genesjc.githubissuefinder.model.LanguageFilter
import io.github.genesjc.githubissuefinder.model.RepoIssueGroup
import io.github.genesjc.githubissuefinder.ui.settings.SettingsScreen
import io.github.genesjc.githubissuefinder.ui.welcome.TokenGuideScreen
import io.github.genesjc.githubissuefinder.ui.welcome.WelcomeScreen
import io.github.genesjc.githubissuefinder.ui.welcome.WelcomeViewModel

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
 * hidden issues and settings. It uses Navigation Compose for real system Back support.
 * The gate is a real start destination inside this NavHost, not an early return outside it:
 * outside, Top Repos was the only entry on the back stack and Back closed the app. The start
 * destination is computed once: the gate if no mode is chosen yet, otherwise Top Repos.
 * The selected repo group and language are plain Compose state held here, not passed through
 * the route. "Reset App" clears the mode and pops the whole back stack, so the gate is not
 * duplicated. More in docs/design-notes.md, "Navigation".
 */
@Composable
fun DiscoverNavHost(
    modifier: Modifier = Modifier,
    issueModeViewModel: IssueModeViewModel = viewModel(),
    welcomeViewModel: WelcomeViewModel = viewModel(),
) {
    val navController = rememberNavController()
    var selectedRepoGroup by remember { mutableStateOf<RepoIssueGroup?>(null) }
    // The language Top Repos was showing, so "Refresh this repo" writes back to the right cache
    // entry. Held here, like selectedRepoGroup, rather than passed through the route.
    var selectedLanguage by remember { mutableStateOf(LanguageFilter.KOTLIN) }

    // The welcome comes first for someone with no token who has not seen it; then the gate if no mode is
    // chosen; otherwise Top Repos.
    val startDestination = remember {
        when {
            welcomeViewModel.needsWelcomeAtStart() -> ROUTE_WELCOME
            needsModeGate(issueModeViewModel.mode.value) -> ROUTE_ISSUE_MODE_GATE
            else -> ROUTE_TOP_REPOS
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(ROUTE_WELCOME) {
            WelcomeScreen(
                onOpenGuide = { navController.navigate(ROUTE_TOKEN_GUIDE) },
                onDone = {
                    if (navController.previousBackStackEntry != null) {
                        // Reopened from the menu: go back to where the user was.
                        navController.popBackStackDebounced()
                    } else {
                        // Shown at start: move on to the gate or Top Repos, and drop the welcome from the back stack.
                        val next = if (needsModeGate(issueModeViewModel.mode.value)) ROUTE_ISSUE_MODE_GATE else ROUTE_TOP_REPOS
                        navController.navigate(next) { popUpTo(ROUTE_WELCOME) { inclusive = true } }
                    }
                },
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
                    selectedRepoGroup = group
                    selectedLanguage = language
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
