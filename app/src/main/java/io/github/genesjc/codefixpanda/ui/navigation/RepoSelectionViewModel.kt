package io.github.genesjc.codefixpanda.ui.navigation

import io.github.genesjc.codefixpanda.model.LanguageFilter
import io.github.genesjc.codefixpanda.model.RepoIssueGroup
import io.github.genesjc.codefixpanda.ui.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds which repo the repo-issues screen is showing. It is Activity-scoped, like IssueModeViewModel,
 * so DiscoverNavHost reads the choice the Top Repos screen reports, and a screen change or a rotation
 * does not lose it. It is not saved across the process being killed: when the back stack is restored
 * after that, [selection] is null and the host returns to Top Repos instead of drawing nothing.
 */
class RepoSelectionViewModel : BaseViewModel() {

    private val _selection = MutableStateFlow<RepoSelection?>(null)

    /** The repo to show, or null before one has been opened (or after the process was restored). */
    val selection: StateFlow<RepoSelection?> = _selection.asStateFlow()

    /** Called when a repo card is opened on Top Repos. */
    fun select(repoGroup: RepoIssueGroup, language: LanguageFilter) {
        _selection.value = RepoSelection(repoGroup, language)
    }
}
