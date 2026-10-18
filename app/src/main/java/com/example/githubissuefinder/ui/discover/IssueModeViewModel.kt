package com.example.githubissuefinder.ui.discover

import com.example.githubissuefinder.data.IssueModeRepository
import com.example.githubissuefinder.model.IssueLabelFilter
import com.example.githubissuefinder.ui.BaseViewModel
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.inject

/**
 * The UI's one interface to the app-wide Issue Mode: the places that
 * need to react to it at the very top of the app (MainActivity, to pick
 * a color scheme) or to change it before any screen-level ViewModel
 * exists (DiscoverNavHost, for the gate screen's choice and for picking
 * the start destination) go through this, never straight to the
 * repository or to storage.
 *
 * Scope: obtained with viewModel() from the root composition, which
 * means it is scoped to the Activity — MainActivity and DiscoverNavHost
 * therefore receive the SAME instance, and the theme re-renders the
 * moment the gate screen's choice lands. (TopReposViewModel and
 * RepoIssuesViewModel are scoped to their own nav destinations, so they
 * (TopReposViewModel through TopReposStateHolder) talk to the repository directly for the same mode rather than going
 * through this one.)
 *
 * Why the repository is a plain member that Koin supplies: production
 * code gets the real one, while a unit test registers its own backed by
 * a fake IssueModeStorage.
 *
 * Why [mode] just re-exposes the repository's StateFlow instead of
 * copying it into its own: there is only one source of truth for the
 * mode, and a second copy could drift out of sync with it.
 */
class IssueModeViewModel : BaseViewModel() {

    private val repository: IssueModeRepository by inject()

    /** The chosen mode, or null while none has been chosen yet (the gate screen is showing). */
    val mode: StateFlow<IssueLabelFilter?> = repository.mode

    /**
     * Called by Reset App: clears the chosen mode so the gate screen is
     * the start destination again. Bookmarks, hidden issues and the repo
     * cache are separate stores and are not touched.
     */
    fun clearMode() {
        repository.setMode(null)
    }

    /** Called when the user picks a mode on the gate screen. Persistence is the repository's job, not this class's. */
    fun selectMode(newMode: IssueLabelFilter) {
        repository.setMode(newMode)
    }
}
