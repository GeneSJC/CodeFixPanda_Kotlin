package com.example.githubissuefinder.ui.discover

import com.example.githubissuefinder.data.AppConfigRepository
import com.example.githubissuefinder.data.BookmarksRepository
import com.example.githubissuefinder.data.HiddenIssuesRepository
import com.example.githubissuefinder.model.Issue
import com.example.githubissuefinder.ui.BaseViewModel
import kotlinx.coroutines.flow.StateFlow

/**
 * The Bookmarks screen's interface to the saved bookmarks: the list to
 * show. What is saved and when is BookmarksRepository's job. Toggling a
 * bookmark (also used for the star on a repo's issue cards) comes from
 * BaseViewModel, since two screens need it.
 *
 * The repository is a constructor argument, supplied by the Koin module,
 * so a test can pass one over a fake storage. It is also the one
 * BaseViewModel reads the bookmarked ids from.
 */
class BookmarksViewModel(
    private val bookmarksRepository: BookmarksRepository,
    appConfigRepository: AppConfigRepository,
    hiddenIssuesRepository: HiddenIssuesRepository,
) : BaseViewModel(appConfigRepository, hiddenIssuesRepository, bookmarksRepository) {

    /** Bookmarked issues, in the order they were saved. */
    val bookmarks: StateFlow<List<Issue>> = bookmarksRepository.bookmarks
}
