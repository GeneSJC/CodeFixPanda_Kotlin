package com.example.githubissuefinder.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.githubissuefinder.data.SampleIssues
import com.example.githubissuefinder.ui.detail.DetailScreen
import com.example.githubissuefinder.ui.feed.FeedScreen

/**
 * Owns navigation between Feed and Detail, plus the bookmark state that
 * both screens need to read/write. This state living here (rather than in
 * each screen) is a deliberate, temporary stand-in for what a shared
 * ViewModel + Room will do once we introduce MVVM — for now, a simple
 * `remember`-held Set<Long> is enough to prove the interaction pattern
 * works before we add that infrastructure.
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

    NavHost(navController = navController, startDestination = "feed") {
        composable("feed") {
            FeedScreen(
                bookmarkedIds = bookmarkedIds,
                onIssueClick = { issueId -> navController.navigate("detail/$issueId") },
            )
        }
        composable(
            route = "detail/{issueId}",
            arguments = listOf(navArgument("issueId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val issueId = backStackEntry.arguments?.getLong("issueId") ?: -1L
            val issue = SampleIssues.all.find { it.id == issueId }

            DetailScreen(
                issue = issue,
                isBookmarked = issueId in bookmarkedIds,
                onBack = { navController.popBackStack() },
                onToggleBookmark = { toggleBookmark(issueId) },
                onOpenInGitHub = { /* real deep link comes once Retrofit provides a real htmlUrl */ },
            )
        }
    }
}
