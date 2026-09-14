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
import com.example.githubissuefinder.ui.feed.FeedScreen
import com.example.githubissuefinder.ui.insights.InsightsScreen

private const val ROUTE_FEED = "feed"
private const val ROUTE_BOOKMARKS = "bookmarks"
private const val ROUTE_INSIGHTS = "insights"
private const val ROUTE_DETAIL = "detail/{issueId}"

/**
 * Owns navigation between Feed, Bookmarks, Insights, and Detail, plus the
 * shared bookmark state the first three screens need.
 *
 * Bookmark state changed shape this Commit Milestone: from Set<Long> to
 * Map<Long, Issue>. Reason: Feed now shows live search results rather than
 * a fixed sample list, so a bookmarked issue can scroll out of the current
 * search entirely (different filters, a later page, etc). Storing the full
 * Issue at the moment it's bookmarked means Bookmarks/Detail keep working
 * for it regardless of what Feed's IssueCache currently holds. This is
 * still just remember-held in-memory state — the real, permanent fix is
 * Room persistence, still a future Commit Milestone.
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
        it.route == ROUTE_FEED || it.route == ROUTE_BOOKMARKS || it.route == ROUTE_INSIGHTS
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
                    NavigationBarItem(
                        selected = currentRoute?.hierarchy?.any { it.route == ROUTE_INSIGHTS } == true,
                        onClick = {
                            navController.navigate(ROUTE_INSIGHTS) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Text("📊", style = MaterialTheme.typography.titleMedium) },
                        label = { Text("Insights") },
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
            composable(ROUTE_INSIGHTS) {
                InsightsScreen()
            }
            composable(
                route = ROUTE_DETAIL,
                arguments = listOf(navArgument("issueId") { type = NavType.LongType }),
            ) { backStackEntry ->
                val issueId = backStackEntry.arguments?.getLong("issueId") ?: -1L
                // Check the bookmark snapshot first (guaranteed available if
                // bookmarked, even if evicted from IssueCache by a later
                // search), then fall back to the transient search cache.
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
