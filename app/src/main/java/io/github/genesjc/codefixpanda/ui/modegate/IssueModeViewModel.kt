package io.github.genesjc.codefixpanda.ui.modegate

import io.github.genesjc.codefixpanda.data.IssueModeRepository
import io.github.genesjc.codefixpanda.domain.needsModeGate
import io.github.genesjc.codefixpanda.model.IssueLabelFilter
import io.github.genesjc.codefixpanda.ui.BaseViewModel
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.inject

/**
 * The UI's one interface to the app-wide Issue Mode, for the places that need it at the very top of
 * the app (MainActivity, for the color scheme; DiscoverNavHost, for the gate and the start
 * destination). It is Activity-scoped, so both receive the same instance and the theme updates the
 * moment the gate's choice lands; the screen ViewModels reach the mode through their state holders.
 * [mode] re-exposes the repository's StateFlow rather than copying it, so there is one source of
 * truth. The repository is a plain member that Koin supplies; a test registers its own over a fake
 * IssueModeStorage.
 */
class IssueModeViewModel : BaseViewModel() {

    private val repository: IssueModeRepository by inject()

    /** The chosen mode, or null while none has been chosen yet (the gate screen is showing). */
    val mode: StateFlow<IssueLabelFilter?> = repository.mode

    /** True while no mode has been chosen, meaning the app must start on the mode gate instead of Top Repos. */
    fun isGateNeeded(): Boolean = needsModeGate(mode.value)

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
