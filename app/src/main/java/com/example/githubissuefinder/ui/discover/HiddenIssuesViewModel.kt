package com.example.githubissuefinder.ui.discover

import com.example.githubissuefinder.data.AppConfigRepository
import com.example.githubissuefinder.data.BookmarksRepository
import com.example.githubissuefinder.data.HiddenIssuesRepository
import com.example.githubissuefinder.model.HiddenIssue
import com.example.githubissuefinder.ui.BaseViewModel
import kotlinx.coroutines.flow.StateFlow

/**
 * The Hidden screen's interface to the hidden issues: the list to show,
 * and Unhide / Unhide all. What is saved and when is
 * HiddenIssuesRepository's job; the confirmation dialog for "Unhide
 * all" is presentational state and stays in the screen.
 *
 * The repository is a constructor argument, supplied by the Koin module,
 * so a test can pass one over a fake storage. It is also the one
 * BaseViewModel reads the hidden ids from.
 */
class HiddenIssuesViewModel(
    private val repository: HiddenIssuesRepository,
    appConfigRepository: AppConfigRepository,
    bookmarksRepository: BookmarksRepository,
) : BaseViewModel(appConfigRepository, repository, bookmarksRepository) {

    /** Hidden issues, most recently hidden first. */
    val hiddenIssues: StateFlow<List<HiddenIssue>> = repository.hiddenIssues

    fun unhide(issueId: Long) {
        repository.unhide(issueId)
    }

    fun unhideAll() {
        repository.unhideAll()
    }
}
