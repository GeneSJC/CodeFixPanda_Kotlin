package com.example.githubissuefinder.ui.discover

import androidx.lifecycle.viewModelScope
import com.example.githubissuefinder.data.ApiTokenRepository
import com.example.githubissuefinder.data.BookmarksRepository
import com.example.githubissuefinder.model.Issue
import com.example.githubissuefinder.ui.BaseViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.inject

/**
 * The Bookmarks screen's interface to the saved bookmarks: the list to
 * show. What is saved and when is BookmarksRepository's job. Only the bookmarks whose tag matches
 * the data in use are listed, so demo bookmarks and real bookmarks never appear together.
 *
 * The repository is a plain member that Koin supplies; a test registers
 * one over a fake storage.
 */
class BookmarksViewModel : BaseViewModel() {

    private val bookmarksRepository: BookmarksRepository by inject()
    private val tokenRepository: ApiTokenRepository by inject()

    /** Bookmarked issues of the data in use, in the order they were saved. */
    val bookmarks: StateFlow<List<Issue>> = combine(bookmarksRepository.bookmarks, tokenRepository.activeTag) { issues, tag ->
        issues.filter { it.tag == tag }
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        bookmarksRepository.bookmarks.value.filter { it.tag == tokenRepository.activeTag.value },
    )

    /** Toggles [issue]'s bookmark; returns the new state (true = now bookmarked). */
    fun toggleBookmark(issue: Issue): Boolean = bookmarksRepository.toggle(issue)
}
