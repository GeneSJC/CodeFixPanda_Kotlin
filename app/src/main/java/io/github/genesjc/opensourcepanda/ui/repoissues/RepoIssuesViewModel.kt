package io.github.genesjc.opensourcepanda.ui.repoissues

import android.app.Activity
import androidx.lifecycle.viewModelScope
import io.github.genesjc.opensourcepanda.R
import io.github.genesjc.opensourcepanda.ads.AdsController
import io.github.genesjc.opensourcepanda.data.ApiTokenRepository
import io.github.genesjc.opensourcepanda.data.BookmarksRepository
import io.github.genesjc.opensourcepanda.data.HiddenIssuesRepository
import io.github.genesjc.opensourcepanda.domain.filterHiddenIssuesFlat
import io.github.genesjc.opensourcepanda.model.Issue
import io.github.genesjc.opensourcepanda.model.IssueLabelFilter
import io.github.genesjc.opensourcepanda.model.LanguageFilter
import io.github.genesjc.opensourcepanda.model.RepoIssueGroup
import io.github.genesjc.opensourcepanda.state.RepoIssuesStateHolder
import io.github.genesjc.opensourcepanda.ui.BaseViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
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
    private val adsController: AdsController by inject()

    init {
        stateHolder.open()
    }

    val uiState: StateFlow<RepoIssuesUiState> = stateHolder.state
        .map { it.toUiState(string) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, stateHolder.state.value.toUiState(string))

    /** True while the demo data is shown (no token), for the banner. */
    val isDemo: StateFlow<Boolean> = tokenRepository.isDemo

    /** True when an ad may be shown on this screen (always false in the open build). */
    val canShowAds: StateFlow<Boolean> = adsController.canShowAds

    /** When the ad-free period ends (epoch milliseconds), or 0; always 0 in the open build. */
    val adFreeUntilMs: StateFlow<Long> = adsController.adFreeUntilMs

    /** True when the user can be offered the ad-free period now (never in the open build). */
    val canOfferAdFree: StateFlow<Boolean> = adsController.canOfferAdFree

    /** Plays the rewarded video behind the ad-free offer. The Activity is used for this call only. */
    fun onAdFreeOffer(activity: Activity) = adsController.showAdFreeOffer(activity)

    /** The mode the screen draws with. Like Top Repos, it starts with the chosen mode and ignores a cleared one (Reset App). */
    val activeMode: StateFlow<IssueLabelFilter> = stateHolder.mode
        .filterNotNull()
        .stateIn(viewModelScope, SharingStarted.Eagerly, stateHolder.requireMode())

    fun loadIfNeeded(repoGroup: RepoIssueGroup) = stateHolder.loadIfNeeded(repoGroup)

    /** The one explicit, user-initiated fetch for this repo; see RepoIssuesStateHolder.refreshThisRepo(). */
    fun refreshThisRepo(repoGroup: RepoIssueGroup, language: LanguageFilter) = stateHolder.refreshThisRepo(repoGroup, language)

    /** The issues the screen lists: [uiState]'s issues with the ones the user hid taken out. The state keeps every issue. */
    val visibleIssues: StateFlow<List<Issue>> =
        combine(uiState, hiddenIssuesRepository.hiddenIds) { state, hiddenIds -> filterHiddenIssuesFlat(state.issues, hiddenIds) }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                filterHiddenIssuesFlat(uiState.value.issues, hiddenIssuesRepository.hiddenIds.value),
            )

    /** Ids of bookmarked issues, to draw each issue card's star. */
    val bookmarkedIds: StateFlow<Set<Long>> = bookmarksRepository.bookmarkedIds

    /** One-shot messages for the screen's Snackbar: a bookmark or hide that could not be saved. */
    val snackbarEvents: Flow<String> = merge(
        bookmarksRepository.saveFailures.map { string(R.string.bookmark_save_failed) },
        hiddenIssuesRepository.saveFailures.map { string(R.string.hidden_save_failed) },
    )

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
