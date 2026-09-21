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

private const val ROUTE_ISSUE_MODE_GATE = "issueModeGate"
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
 * short, self-contained overlay; this is a genuine set of screens a
 * user will expect to back out of properly.
 *
 * The selected Repo is held as plain Compose state at THIS level,
 * NOT serialized through the nav route itself — avoids needing
 * Parcelable/Serializable ceremony on the Repo model just to pass it
 * through a nav argument, since the full object is already in memory
 * from the ranked list TopReposScreen loaded.
 *
 * v0.41 ("Issue Mode" shell) — REVISED after a real bug found running
 * the app, not caught in review: the gate is a REAL destination in
 * this NavHost (ROUTE_ISSUE_MODE_GATE), reached as the actual
 * startDestination, rather than an early `return` outside the NavHost
 * entirely (the first version of this file). That first version built
 * and ran fine, but broke system back navigation — with the gate
 * living OUTSIDE the NavHost, Top Repos was the only thing ever pushed
 * onto ITS back stack, so pressing back from Top Repos had nothing to
 * pop; the press propagated past this composable, past MainActivity,
 * and closed the app to the device home screen instead of returning to
 * the gate. This is exactly the kind of thing that only shows up by
 * actually pressing back on a running device, not from reading the
 * code — confirmed here by screenshots of it happening.
 *
 * The fix: IssueModeGateScreen is now composable(ROUTE_ISSUE_MODE_GATE)
 * inside this same NavHost. startDestination is computed once via
 * `remember` — NOT re-read reactively off IssueMode.current on every
 * recomposition, since NavHost's startDestination is fixed at creation
 * time and isn't how Navigation Compose expects to be driven after the
 * fact. Picking a mode on the gate screen calls
 * navController.navigate(ROUTE_TOP_REPOS), which by default PUSHES Top
 * Repos on top of the gate rather than replacing it (no popUpTo is
 * specified) — so the gate stays underneath in the back stack, and
 * system back from Top Repos now correctly pops back to it, letting
 * someone change their mind about Mode instead of being dropped out of
 * the app entirely. RepoIssuesScreen's own back behavior (popBackStack
 * back to Top Repos) is unchanged by any of this.
 *
 * Mode isn't persisted across process death yet (see IssueMode.kt), so
 * in practice startDestination is always ROUTE_ISSUE_MODE_GATE on a
 * fresh launch today — the IssueMode.current != null branch exists for
 * forward-compatibility with whenever persistence lands, not because
 * it's reachable right now.
 */
@Composable
fun DiscoverNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    var selectedRepo by remember { mutableStateOf<Repo?>(null) }

    val startDestination = remember {
        if (IssueMode.current != null) ROUTE_TOP_REPOS else ROUTE_ISSUE_MODE_GATE
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(ROUTE_ISSUE_MODE_GATE) {
            IssueModeGateScreen(
                onModeSelected = { mode ->
                    IssueMode.current = mode
                    navController.navigate(ROUTE_TOP_REPOS)
                },
            )
        }
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
