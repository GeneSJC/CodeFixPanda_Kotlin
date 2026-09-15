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
import com.example.githubissuefinder.data.IssueCache
import com.example.githubissuefinder.model.Issue
import com.example.githubissuefinder.ui.bookmarks.BookmarksScreen
import com.example.githubissuefinder.ui.detail.DetailScreen
import com.example.githubissuefinder.ui.feed.FeedTabsScreen

private const val ROUTE_FEED = "feed"
private const val ROUTE_BOOKMARKS = "bookmarks"
private const val ROUTE_DETAIL = "detail/{issueId}"

/**
 * Owns navigation between Feed, Bookmarks, and Detail, plus the shared
 * bookmark state. Bottom nav is now 2 top-level items (Feed, Bookmarks) —
 * Insights was removed as its own bottom-nav destination and is now a top
 * tab inside FeedTabsScreen alongside the issue list, since it was never
 * conceptually a separate place, just a different lens on Feed's current
 * data. Detail is still pushed on top without the bottom bar, same as
 * before.
 *
 * Bookmark state is still a simple remember-held Map<Long, Issue> (see
 * v0.8 for why it's a Map, not a Set<Long>) — a deliberate, temporary
 * stand-in for what a shared ViewModel + Room will do once we introduce
 * MVVM/Hilt/Room fully.
 */
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    var bookmarkedIssues by remember { mutableStateOf(mapOf<Long, Issue>()) }

    fun toggleBookmark(issueId: Long) {
        bookmarkedIssues = if (issueId in bookmarkedIssues) {
            bookmarkedIssues - issueId
        } else {
            val issue = IssueCache.get(issueId)
            if (issue != null) bookmarkedIssues + (issueId to issue) else bookmarkedIssues
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
                FeedTabsScreen(
                    bookmarkedIds = bookmarkedIssues.keys,
                    onIssueClick = { issueId -> navController.navigate("detail/$issueId") },
                )
            }
            composable(ROUTE_BOOKMARKS) {
                BookmarksScreen(
                    bookmarkedIssues = bookmarkedIssues.values.toList(),
                    onIssueClick = { issueId -> navController.navigate("detail/$issueId") },
                )
            }
            composable(
                route = ROUTE_DETAIL,
                arguments = listOf(navArgument("issueId") { type = NavType.LongType }),
            ) { backStackEntry ->
                val issueId = backStackEntry.arguments?.getLong("issueId") ?: -1L
                val issue = bookmarkedIssues[issueId] ?: IssueCache.get(issueId)

                DetailScreen(
                    issue = issue,
                    isBookmarked = issueId in bookmarkedIssues,
                    onBack = { navController.popBackStack() },
                    onToggleBookmark = { toggleBookmark(issueId) },
                )
            }
        }
    }
}
