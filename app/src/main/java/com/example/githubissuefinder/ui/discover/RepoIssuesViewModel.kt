package com.example.githubissuefinder.ui.discover

import androidx.lifecycle.viewModelScope
import com.example.githubissuefinder.data.HiddenIssuesRepository
import com.example.githubissuefinder.data.RepoIssuesService
import com.example.githubissuefinder.model.Issue
import com.example.githubissuefinder.model.IssueLabelFilter
import com.example.githubissuefinder.model.LanguageFilter
import com.example.githubissuefinder.model.RepoIssueGroup
import com.example.githubissuefinder.ui.BaseViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * The view model for RepoIssuesScreen, the repo-scoped issue list: it
 * maps the repo-issues data state to what the screen shows, and forwards
 * the user's actions.
 *
 * The data state itself, the logic that loads and refreshes it, and the
 * long design history of this screen (why it makes no network call until
 * the user asks, the "Refresh this repo" write-back to the Top Repos
 * cache) all live in RepoIssuesService. This class holds none of that
 * state: [uiState] is the service's state with the message text added
 * (see RepoIssuesUiMapping.kt).
 *
 * Constructing this class starts a fresh visit to the screen
 * (RepoIssuesService.open()), as constructing it has always meant.
 *
 * The service and the hidden-issue repository are plain members, the
 * app-wide instances; tests install their own with
 * RepoIssuesService.replaceInstanceForTests() and
 * HiddenIssuesRepository.replaceInstanceForTests(). The hidden ids, the
 * bookmarked ids and the bookmark toggle come from BaseViewModel.
 */
class RepoIssuesViewModel : BaseViewModel() {

    private val service = RepoIssuesService.instance

    /** The hidden-issue repository, for hiding and unhiding. The hidden ids themselves come from BaseViewModel. */
    private val hiddenIssuesRepository = HiddenIssuesRepository.instance

    init {
        service.open()
    }

    val uiState: StateFlow<RepoIssuesUiState> = service.state
        .map { it.toUiState() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, service.state.value.toUiState())

    /** The active Issue Mode, so RepoIssuesScreen observes it through this ViewModel instead of reading a global. */
    val mode: StateFlow<IssueLabelFilter?> = service.mode

    fun loadIfNeeded(repoGroup: RepoIssueGroup) = service.loadIfNeeded(repoGroup)

    /** The one explicit, user-initiated fetch for this repo; see RepoIssuesService.refreshThisRepo(). */
    fun refreshThisRepo(repoGroup: RepoIssueGroup, language: LanguageFilter) = service.refreshThisRepo(repoGroup, language)

    fun hideIssue(issue: Issue) {
        hiddenIssuesRepository.hide(issue)
    }

    /** Used by the "Undo" on the "Issue hidden" Snackbar. */
    fun unhideIssue(issueId: Long) {
        hiddenIssuesRepository.unhide(issueId)
    }
}
