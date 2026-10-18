package com.example.githubissuefinder.ui.discover

import com.example.githubissuefinder.data.HiddenIssuesRepository
import com.example.githubissuefinder.model.HiddenIssue
import com.example.githubissuefinder.ui.BaseViewModel
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.inject

/**
 * The Hidden screen's interface to the hidden issues: the list to show,
 * and Unhide / Unhide all. What is saved and when is
 * HiddenIssuesRepository's job; the confirmation dialog for "Unhide
 * all" is presentational state and stays in the screen.
 *
 * The repository is a plain member that Koin supplies; a test registers
 * one over a fake storage.
 */
class HiddenIssuesViewModel : BaseViewModel() {

    private val repository: HiddenIssuesRepository by inject()

    /** Hidden issues, most recently hidden first. */
    val hiddenIssues: StateFlow<List<HiddenIssue>> = repository.hiddenIssues

    fun unhide(issueId: Long) {
        repository.unhide(issueId)
    }

    fun unhideAll() {
        repository.unhideAll()
    }
}
