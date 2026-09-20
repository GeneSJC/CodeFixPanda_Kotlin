package com.example.githubissuefinder.ui.discover

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.githubissuefinder.model.Repo

private const val ROUTE_TOP_REPOS = "topRepos"
private const val ROUTE_REPO_ISSUES = "repoIssues"

/**
 * Navigation host for the "repoFirst" Discover flow — introduced in
 * v0.38 alongside RepoIssuesScreen, the drill-in every Discover CM
 * since v0.25 had flagged as deferred (tapping a repo was a no-op).
 *
 * Uses Navigation Compose (already a dependency, already proven by
 * "classic"'s AppNavHost — no new library risk) specifically for real
 * system back-button support, unlike the simple boolean-state-swap
 * pattern used for the splash-replay overlay (v0.31) — that was a
 * short, self-contained overlay; this is a genuine second screen a
 * user will expect to back out of properly.
 *
 * The selected Repo is held as plain Compose state at THIS level,
 * NOT serialized through the nav route itself — avoids needing
 * Parcelable/Serializable ceremony on the Repo model just to pass it
 * through a nav argument, since the full object is already in memory
 * from the ranked list TopReposScreen loaded.
 */
@Composable
fun DiscoverNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    var selectedRepo by remember { mutableStateOf<Repo?>(null) }

    NavHost(
        navController = navController,
        startDestination = ROUTE_TOP_REPOS,
        modifier = modifier,
    ) {
        composable(ROUTE_TOP_REPOS) {
            TopReposScreen(
                onRepoClick = { repo ->
                    selectedRepo = repo
                    navController.navigate(ROUTE_REPO_ISSUES)
                },
            )
        }
        composable(ROUTE_REPO_ISSUES) {
            selectedRepo?.let { repo ->
                RepoIssuesScreen(
                    repo = repo,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
