package com.example.githubissuefinder.ui.discover

import androidx.lifecycle.viewModelScope
import com.example.githubissuefinder.data.AppConfigRepository
import com.example.githubissuefinder.data.HiddenIssuesRepository
import com.example.githubissuefinder.data.RateLimitInfo
import com.example.githubissuefinder.data.ReadmeSnippetRepository
import com.example.githubissuefinder.data.TopReposService
import com.example.githubissuefinder.model.IssueLabelFilter
import com.example.githubissuefinder.model.LanguageFilter
import com.example.githubissuefinder.ui.BaseViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The view model for TopReposScreen: it maps the Top Repos data state
 * to what the screen shows, and forwards the user's actions.
 *
 * The data state itself, the logic that loads and changes it, and the
 * long design history of how the list is built (the v0.42 data-layer
 * swap, pagination and cross-page dedup, the persistent cache, the
 * cache-preserving refresh) all live in TopReposService. This class
 * holds none of that state: [uiState] is the service's state with the
 * message text added (see TopReposUiMapping.kt), and [snackbarEvents]
 * are the service's events turned into text.
 *
 * Constructing this class starts a fresh visit to the screen
 * (TopReposService.open()), which is what constructing it has always
 * meant: load the list for the default language, cache first.
 *
 * The service is a plain member, the app-wide TopReposService; tests
 * install their own with TopReposService.replaceInstanceForTests().
 * The remaining arguments serve the repo cards (README previews, hidden
 * issues) and are not part of the list's data state.
 */
class TopReposViewModel(
    private val hiddenIssuesRepository: HiddenIssuesRepository = HiddenIssuesRepository.instance,
    private val snippetRepository: ReadmeSnippetRepository = ReadmeSnippetRepository.instance,
    appConfigRepository: AppConfigRepository = AppConfigRepository.instance,
) : BaseViewModel(appConfigRepository) {

    private val service = TopReposService.instance

    init {
        service.open()
    }

    val uiState: StateFlow<TopReposUiState> = service.state
        .map { it.toUiState() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, service.state.value.toUiState())

    /** One-shot messages for the screen's Snackbar, each delivered exactly once. */
    val snackbarEvents: Flow<String> = service.events.map { it.toMessage() }

    /** README preview text by repo full name, for the repo cards (a null value means "no preview"). */
    val snippets: StateFlow<Map<String, String?>> = snippetRepository.snippets

    /** The Search API rate-limit numbers for the chip, or null until they are known. */
    val rateLimit: StateFlow<RateLimitInfo?> = service.rateLimit

    /** Whether a token is in use right now, for the rate-limit chip's tap (a token problem, or the real dialog). */
    val hasToken: StateFlow<Boolean> = service.hasToken

    /** Ids the user hid, to filter out of the repo list at display time. */
    val hiddenIds: StateFlow<Set<Long>> = hiddenIssuesRepository.hiddenIds

    /**
     * The active Issue Mode, or null while none is chosen (which
     * DiscoverNavHost prevents from ever being observed here — see
     * IssueModeGateScreen).
     */
    val mode: StateFlow<IssueLabelFilter?> = service.mode

    /**
     * A repo card appeared on screen: load its README preview if it
     * hasn't been loaded yet. The screen only reports the event; the
     * network request and the retry rules live in ReadmeSnippetRepository.
     */
    fun onRepoCardShown(repoOwner: String, repoName: String, repoFullName: String) {
        viewModelScope.launch { snippetRepository.loadIfNeeded(repoOwner, repoName, repoFullName) }
    }

    fun onLanguageSelected(language: LanguageFilter) = service.selectLanguage(language)

    /**
     * Handler for the two hamburger-menu items TopReposScreen offers
     * ("Find Good First Issues" / "Find Help Wanted Issues"): a direct,
     * in-place switch rather than navigating anywhere. See
     * TopReposScreen's doc comment for the full reasoning, including why
     * this is not a return to IssueModeGateScreen.
     */
    fun onModeSelected(mode: IssueLabelFilter) = service.selectMode(mode)

    /** Retries the INITIAL load — for when the list is empty because the first fetch failed. Unrelated to retryLoadMore(), which retries a failed Load More instead. */
    fun retry() = service.retry()

    /** Fetches the next page and adds it to what's shown; safe to wire straight to a button (see TopReposService.loadMore()). */
    fun loadMore() = service.loadMore()

    /** Retries a Load More that failed — distinct from retry(), which is for the initial load. */
    fun retryLoadMore() = service.retryLoadMore()

    /** "Check for New Repos": see TopReposService.checkForNewRepos() for what it does and what it leaves untouched. */
    fun checkForNewRepos() = service.checkForNewRepos()

    /** Clears the ENTIRE persistent cache and reloads; see TopReposService.clearCache(). */
    fun clearCache() = service.clearCache()

    /** Pulls a change made elsewhere (a repo refreshed from RepoIssuesScreen) back into the list; see TopReposService.syncFromCache(). */
    fun syncFromCache() = service.syncFromCache()
}
