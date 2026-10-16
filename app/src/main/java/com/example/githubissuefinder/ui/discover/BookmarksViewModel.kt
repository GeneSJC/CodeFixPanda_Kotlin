package com.example.githubissuefinder.ui.discover

import com.example.githubissuefinder.data.BookmarksRepository
import com.example.githubissuefinder.model.Issue
import com.example.githubissuefinder.ui.BaseViewModel
import kotlinx.coroutines.flow.StateFlow

/**
 * The Bookmarks screen's interface to the saved bookmarks: the list to
 * show. What is saved and when is BookmarksRepository's job. Toggling a
 * bookmark (also used for the star on a repo's issue cards) comes from
 * BaseViewModel, since two screens need it.
 *
 * The repository is a plain member, the app-wide instance; a test
 * installs one over a fake storage with
 * BookmarksRepository.replaceInstanceForTests().
 */
class BookmarksViewModel : BaseViewModel() {

    /** Bookmarked issues, in the order they were saved. */
    val bookmarks: StateFlow<List<Issue>> = BookmarksRepository.instance.bookmarks
}
