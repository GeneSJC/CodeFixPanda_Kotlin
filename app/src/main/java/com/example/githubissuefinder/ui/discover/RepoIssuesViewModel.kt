package com.example.githubissuefinder.ui.discover

import androidx.lifecycle.viewModelScope
import com.example.githubissuefinder.data.AppConfig
import com.example.githubissuefinder.data.BookmarksRepository
import com.example.githubissuefinder.data.HiddenIssuesRepository
import com.example.githubissuefinder.data.RepoIssuesStateHolder
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
 * cache) all live in RepoIssuesStateHolder. This class holds none of that
 * state: [uiState] is the state holder's state with the message text added
 * (see RepoIssuesUiMapping.kt).
 *
 * Constructing this class starts a fresh visit to the screen
 * (RepoIssuesStateHolder.open()), as constructing it has always meant.
 *
 * The state holder and the two repositories are constructor arguments,
 * supplied by the Koin module; tests pass their own. The hidden-issue
 * repository serves hiding and unhiding and the hidden ids the list is
 * filtered by; the bookmark repository serves the star on each card.
 */
class RepoIssuesViewModel(
    private val stateHolder: RepoIssuesStateHolder,
    private val hiddenIssuesRepository: HiddenIssuesRepository,
    private val bookmarksRepository: BookmarksRepository,
    appConfig: AppConfig,
) : BaseViewModel(appConfig) {

    init {
        stateHolder.open()
    }

    val uiState: StateFlow<RepoIssuesUiState> = stateHolder.state
        .map { it.toUiState() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, stateHolder.state.value.toUiState())

    /** The active Issue Mode, so RepoIssuesScreen observes it through this ViewModel instead of reading a global. */
    val mode: StateFlow<IssueLabelFilter?> = stateHolder.mode

    fun loadIfNeeded(repoGroup: RepoIssueGroup) = stateHolder.loadIfNeeded(repoGroup)

    /** The one explicit, user-initiated fetch for this repo; see RepoIssuesStateHolder.refreshThisRepo(). */
    fun refreshThisRepo(repoGroup: RepoIssueGroup, language: LanguageFilter) = stateHolder.refreshThisRepo(repoGroup, language)

    /** Ids the user hid, to filter hidden issues out of the list at display time. */
    val hiddenIds: StateFlow<Set<Long>> = hiddenIssuesRepository.hiddenIds

    /** Ids of bookmarked issues, to draw each issue card's star. */
    val bookmarkedIds: StateFlow<Set<Long>> = bookmarksRepository.bookmarkedIds

    /** Toggles [issue]'s bookmark; returns the new state (true = now bookmarked). */
    fun toggleBookmark(issue: Issue): Boolean = bookmarksRepository.toggle(issue)

    fun hideIssue(issue: Issue) {
        hiddenIssuesRepository.hide(issue)
    }

    /** Used by the "Undo" on the "Issue hidden" Snackbar. */
    fun unhideIssue(issueId: Long) {
        hiddenIssuesRepository.unhide(issueId)
    }
}
