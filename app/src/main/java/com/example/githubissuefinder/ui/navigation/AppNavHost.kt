package com.example.githubissuefinder.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.githubissuefinder.data.SampleIssues
import com.example.githubissuefinder.ui.bookmarks.BookmarksScreen
import com.example.githubissuefinder.ui.detail.DetailScreen
import com.example.githubissuefinder.ui.feed.FeedScreen

private const val ROUTE_FEED = "feed"
private const val ROUTE_BOOKMARKS = "bookmarks"
private const val ROUTE_DETAIL = "detail/{issueId}"

/**
 * Owns navigation between Feed, Bookmarks, and Detail, plus the shared
 * bookmark state all three screens need. The bottom nav bar only appears
 * on the two top-level destinations (Feed, Bookmarks) — Detail is pushed
 * on top without it, which is the standard pattern for a "drill-in" screen.
 *
 * Bookmark state is still a simple remember-held Set<Long>, same as v0.3 —
 * this is a deliberate, temporary stand-in for what a shared ViewModel +
 * Room will do once we introduce MVVM.
 */
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    var bookmarkedIds by remember { mutableStateOf(setOf<Long>()) }

    fun toggleBookmark(issueId: Long) {
        bookmarkedIds = if (issueId in bookmarkedIds) {
            bookmarkedIds - issueId
        } else {
            bookmarkedIds + issueId
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination
    val showBottomBar = currentRoute?.hierarchy?.any {
        it.route == ROUTE_FEED || it.route == ROUTE_BOOKMARKS
    } == true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute?.hierarchy?.any { it.route == ROUTE_FEED } == true,
                        onClick = {
                            navController.navigate(ROUTE_FEED) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Text("📋", style = MaterialTheme.typography.titleMedium) },
                        label = { Text("Feed") },
                    )
                    NavigationBarItem(
                        selected = currentRoute?.hierarchy?.any { it.route == ROUTE_BOOKMARKS } == true,
                        onClick = {
                            navController.navigate(ROUTE_BOOKMARKS) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Text("🔖", style = MaterialTheme.typography.titleMedium) },
                        label = { Text("Bookmarks") },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = ROUTE_FEED,
            modifier = Modifier.padding(padding),
        ) {
            composable(ROUTE_FEED) {
                FeedScreen(
                    bookmarkedIds = bookmarkedIds,
                    onIssueClick = { issueId -> navController.navigate("detail/$issueId") },
                )
            }
            composable(ROUTE_BOOKMARKS) {
                val bookmarkedIssues = SampleIssues.all.filter { it.id in bookmarkedIds }
                BookmarksScreen(
                    bookmarkedIssues = bookmarkedIssues,
                    onIssueClick = { issueId -> navController.navigate("detail/$issueId") },
                )
            }
            composable(
                route = ROUTE_DETAIL,
                arguments = listOf(navArgument("issueId") { type = NavType.LongType }),
            ) { backStackEntry ->
                val issueId = backStackEntry.arguments?.getLong("issueId") ?: -1L
                val issue = SampleIssues.all.find { it.id == issueId }

                DetailScreen(
                    issue = issue,
                    isBookmarked = issueId in bookmarkedIds,
                    onBack = { navController.popBackStack() },
                    onToggleBookmark = { toggleBookmark(issueId) },
                )
            }
        }
    }
}
