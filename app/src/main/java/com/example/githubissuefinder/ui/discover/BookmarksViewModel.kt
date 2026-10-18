package com.example.githubissuefinder.ui.discover

import com.example.githubissuefinder.data.AppConfig
import com.example.githubissuefinder.data.BookmarksRepository
import com.example.githubissuefinder.model.Issue
import com.example.githubissuefinder.ui.BaseViewModel
import kotlinx.coroutines.flow.StateFlow

/**
 * The Bookmarks screen's interface to the saved bookmarks: the list to
 * show. What is saved and when is BookmarksRepository's job.
 *
 * The repository is a constructor argument, supplied by the Koin module,
 * so a test can pass one over a fake storage.
 */
class BookmarksViewModel(
    private val bookmarksRepository: BookmarksRepository,
    appConfig: AppConfig,
) : BaseViewModel(appConfig) {

    /** Bookmarked issues, in the order they were saved. */
    val bookmarks: StateFlow<List<Issue>> = bookmarksRepository.bookmarks

    /** Toggles [issue]'s bookmark; returns the new state (true = now bookmarked). */
    fun toggleBookmark(issue: Issue): Boolean = bookmarksRepository.toggle(issue)
}
