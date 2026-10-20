package io.github.genesjc.githubissuefinder.ui.discover

import androidx.lifecycle.viewModelScope
import io.github.genesjc.githubissuefinder.data.ApiTokenRepository
import io.github.genesjc.githubissuefinder.data.BookmarksRepository
import io.github.genesjc.githubissuefinder.data.HiddenIssuesRepository
import io.github.genesjc.githubissuefinder.data.RepoIssuesStateHolder
import io.github.genesjc.githubissuefinder.model.Issue
import io.github.genesjc.githubissuefinder.model.IssueLabelFilter
import io.github.genesjc.githubissuefinder.model.LanguageFilter
import io.github.genesjc.githubissuefinder.model.RepoIssueGroup
import io.github.genesjc.githubissuefinder.ui.BaseViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.inject

/**
 * The ViewModel for RepoIssuesScreen: maps the repo-issues state to what the screen shows and
 * forwards actions. The state and the logic live in RepoIssuesStateHolder; [uiState] is its state
 * with the message text added (RepoIssuesUiMapping.kt). Constructing it starts a fresh visit
 * (open()). The state holder and the two repositories are plain members that Koin supplies: the
 * hidden-issue repository serves hiding, unhiding and the hidden ids, and the bookmark repository
 * the star on each card.
 */
class RepoIssuesViewModel : BaseViewModel() {

    private val stateHolder: RepoIssuesStateHolder by inject()
    private val hiddenIssuesRepository: HiddenIssuesRepository by inject()
    private val bookmarksRepository: BookmarksRepository by inject()
    private val tokenRepository: ApiTokenRepository by inject()

    init {
        stateHolder.open()
    }

    val uiState: StateFlow<RepoIssuesUiState> = stateHolder.state
        .map { it.toUiState(string) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, stateHolder.state.value.toUiState(string))

    /** True while the demo data is shown (no token), for the banner. */
    val isDemo: StateFlow<Boolean> = tokenRepository.isDemo

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
