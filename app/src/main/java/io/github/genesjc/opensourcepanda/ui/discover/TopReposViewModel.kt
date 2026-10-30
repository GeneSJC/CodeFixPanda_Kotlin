package io.github.genesjc.opensourcepanda.ui.discover

import android.app.Activity
import androidx.lifecycle.viewModelScope
import io.github.genesjc.opensourcepanda.ads.AdsController
import io.github.genesjc.opensourcepanda.data.ApiTokenRepository
import io.github.genesjc.opensourcepanda.data.HiddenIssuesRepository
import io.github.genesjc.opensourcepanda.data.RateLimitInfo
import io.github.genesjc.opensourcepanda.data.ReadmeSnippetRepository
import io.github.genesjc.opensourcepanda.data.TopReposStateHolder
import io.github.genesjc.opensourcepanda.model.IssueLabelFilter
import io.github.genesjc.opensourcepanda.model.LanguageFilter
import io.github.genesjc.opensourcepanda.ui.BaseViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.inject

/**
 * The ViewModel for TopReposScreen: maps the Top Repos state to what the screen shows and forwards
 * actions. The state and the logic live in TopReposStateHolder; [uiState] is its state with the
 * message text added (TopReposUiMapping.kt) and [snackbarEvents] are its events turned into text.
 * Constructing it starts a fresh visit (open()): load the default language, cache first. The state
 * holder and repositories are plain members that Koin supplies; the snippet repository serves the
 * card previews and the hidden-issue repository the hidden ids.
 */
class TopReposViewModel : BaseViewModel() {

    private val stateHolder: TopReposStateHolder by inject()
    private val snippetRepository: ReadmeSnippetRepository by inject()
    private val hiddenIssuesRepository: HiddenIssuesRepository by inject()
    private val tokenRepository: ApiTokenRepository by inject()
    private val adsController: AdsController by inject()

    init {
        stateHolder.open()
    }

    val uiState: StateFlow<TopReposUiState> = stateHolder.state
        .map { it.toUiState(string) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, stateHolder.state.value.toUiState(string))

    /** One-shot messages for the screen's Snackbar, each delivered exactly once. */
    val snackbarEvents: Flow<String> = stateHolder.events.map { it.toMessage(string) }

    /** Ids the user hid, to filter hidden issues out of the list at display time. */
    val hiddenIds: StateFlow<Set<Long>> = hiddenIssuesRepository.hiddenIds

    /** README preview text by repo full name, for the repo cards (a null value means "no preview"). */
    val snippets: StateFlow<Map<String, String?>> = snippetRepository.snippets

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

    /** True when the menu must offer a way to reopen the privacy choices (never in the open build). */
    val privacyOptionsRequired: StateFlow<Boolean> = adsController.privacyOptionsRequired

    /** Reopens the privacy choices. The Activity is used for this call only. */
    fun onPrivacyOptions(activity: Activity) = adsController.showPrivacyOptions(activity)

    /** The Search API rate-limit numbers for the chip, or null until they are known. */
    val rateLimit: StateFlow<RateLimitInfo?> = stateHolder.rateLimit

    /** Whether a token is in use right now, for the rate-limit chip's tap (a token problem, or the real dialog). */
    val hasToken: StateFlow<Boolean> = stateHolder.hasToken

    /**
     * The active Issue Mode, or null while none is chosen (which
     * DiscoverNavHost prevents from ever being observed here — see
     * IssueModeGateScreen).
     */
    val mode: StateFlow<IssueLabelFilter?> = stateHolder.mode

    /**
     * A repo card appeared on screen: load its README preview if it
     * hasn't been loaded yet. The screen only reports the event; the
     * network request and the retry rules live in ReadmeSnippetRepository.
     */
    fun onRepoCardShown(repoOwner: String, repoName: String, repoFullName: String) {
        viewModelScope.launch { snippetRepository.loadIfNeeded(repoOwner, repoName, repoFullName) }
    }

    fun onLanguageSelected(language: LanguageFilter) = stateHolder.selectLanguage(language)

    /**
     * Handler for the two navigation-drawer items TopReposScreen offers
     * ("Find Beginner-Friendly Issues" / "Find Projects Asking for Help"): a direct,
     * in-place switch rather than navigating anywhere. See
     * TopReposScreen's doc comment for the full reasoning, including why
     * this is not a return to IssueModeGateScreen.
     */
    fun onModeSelected(mode: IssueLabelFilter) = stateHolder.selectMode(mode)

    /** Retries the INITIAL load — for when the list is empty because the first fetch failed. Unrelated to retryLoadMore(), which retries a failed Load More instead. */
    fun retry() = stateHolder.retry()

    /** Fetches the next page and adds it to what's shown; safe to wire straight to a button (see TopReposStateHolder.loadMore()). */
    fun loadMore() = stateHolder.loadMore()

    /** Retries a Load More that failed — distinct from retry(), which is for the initial load. */
    fun retryLoadMore() = stateHolder.retryLoadMore()

    /** "Check for New Repos": see TopReposStateHolder.checkForNewRepos() for what it does and what it leaves untouched. */
    fun checkForNewRepos() = stateHolder.checkForNewRepos()

    /** Clears the ENTIRE persistent cache and reloads; see TopReposStateHolder.clearCache(). */
    fun clearCache() = stateHolder.clearCache()

    /** Pulls a change made elsewhere (a repo refreshed from RepoIssuesScreen) back into the list; see TopReposStateHolder.syncFromCache(). */
    fun syncFromCache() = stateHolder.syncFromCache()
}
