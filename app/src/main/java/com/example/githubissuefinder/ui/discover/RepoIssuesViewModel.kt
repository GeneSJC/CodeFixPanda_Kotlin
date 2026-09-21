package com.example.githubissuefinder.ui.discover

import androidx.lifecycle.ViewModel
import com.example.githubissuefinder.model.RepoIssueGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The repo-scoped issue list — CM "B" of the Discover redesign
 * (option C from the architecture discussion): the drill-in that
 * every Discover CM since v0.25 has flagged as deferred, since tapping
 * a repo was previously a no-op.
 *
 * v0.42, REVISED — the in-screen label picker (LabelFilterRow, which
 * let someone peek at the OTHER label within a repo, independent of
 * the app's active Issue Mode) is REMOVED. The original draft of this
 * CM had explicitly flagged that as "a real, open question, not
 * decided in this CM" — it's now been decided: Mode is a single,
 * deliberate, whole-app choice made once at the gate screen, and
 * nothing downstream offers a second way to change it, no matter how
 * many levels deep into the app. Same reasoning that removed the Top
 * Repos mode dropdown, applied here for consistency.
 *
 * The direct consequence: this ViewModel no longer makes ANY network
 * call, ever, for any reason. Every issue this screen could possibly
 * show is already sitting in repoGroup.issues — that's exactly what a
 * repo showing up on Top Repos at all already means (see
 * RepoIssueGroup). loadIfNeeded() is a pure, synchronous assignment.
 * onLabelSelected(), load(), and retry() are gone entirely, not just
 * simplified — the only thing that ever called them was the label
 * picker, and once that's gone they're dead code, not kept "just in
 * case."
 *
 * Still a ViewModel, not just a `remember` in the Composable, for
 * consistency with every other screen in this app rather than because
 * this one specifically needs ViewModel scoping for anything —
 * loadedForRepoFullName exists purely so navigating away and back
 * within the same process doesn't redundantly reassign issues to the
 * value they already held.
 */
class RepoIssuesViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RepoIssuesUiState())
    val uiState: StateFlow<RepoIssuesUiState> = _uiState.asStateFlow()

    private var loadedForRepoFullName: String? = null

    fun loadIfNeeded(repoGroup: RepoIssueGroup) {
        if (loadedForRepoFullName == repoGroup.repoFullName) return
        loadedForRepoFullName = repoGroup.repoFullName
        _uiState.update { it.copy(issues = repoGroup.issues) }
    }
}
