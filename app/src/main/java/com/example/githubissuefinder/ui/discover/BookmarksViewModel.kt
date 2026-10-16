package com.example.githubissuefinder.ui.discover

import androidx.lifecycle.ViewModel
import com.example.githubissuefinder.data.BookmarksRepository
import com.example.githubissuefinder.model.Issue
import kotlinx.coroutines.flow.StateFlow

/**
 * The Bookmarks screen's interface to the saved bookmarks: the list to
 * show, and removing one. What is saved and when is BookmarksRepository's
 * job. (RepoIssuesViewModel exposes the same bookmarked ids and toggle
 * for the star on a repo's issue cards.)
 *
 * The repository is a constructor argument defaulting to the app-wide
 * instance so a test can pass one over a fake storage; a ViewModel whose
 * constructor arguments all have defaults still has the no-argument
 * constructor the default factory needs.
 */
class BookmarksViewModel(
    private val repository: BookmarksRepository = BookmarksRepository.instance,
) : ViewModel() {

    /** Bookmarked issues, in the order they were saved. */
    val bookmarks: StateFlow<List<Issue>> = repository.bookmarks

    /** Toggles [issue]'s bookmark; returns the new state (true = now bookmarked). */
    fun toggleBookmark(issue: Issue): Boolean = repository.toggle(issue)
}
