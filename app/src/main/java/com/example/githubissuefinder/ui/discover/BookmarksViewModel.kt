package com.example.githubissuefinder.ui.discover

import com.example.githubissuefinder.data.BookmarksRepository
import com.example.githubissuefinder.model.Issue
import com.example.githubissuefinder.ui.BaseViewModel
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.inject

/**
 * The Bookmarks screen's interface to the saved bookmarks: the list to
 * show. What is saved and when is BookmarksRepository's job.
 *
 * The repository is a plain member that Koin supplies; a test registers
 * one over a fake storage.
 */
class BookmarksViewModel : BaseViewModel() {

    private val bookmarksRepository: BookmarksRepository by inject()

    /** Bookmarked issues, in the order they were saved. */
    val bookmarks: StateFlow<List<Issue>> = bookmarksRepository.bookmarks

    /** Toggles [issue]'s bookmark; returns the new state (true = now bookmarked). */
    fun toggleBookmark(issue: Issue): Boolean = bookmarksRepository.toggle(issue)
}
