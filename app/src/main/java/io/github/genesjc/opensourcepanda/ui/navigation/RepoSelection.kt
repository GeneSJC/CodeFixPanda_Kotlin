package io.github.genesjc.opensourcepanda.ui.navigation

import io.github.genesjc.opensourcepanda.model.LanguageFilter
import io.github.genesjc.opensourcepanda.model.RepoIssueGroup

/**
 * The repo the user opened from Top Repos, with the language Top Repos was showing, so the repo screen
 * can show its issues and "Refresh this repo" writes back to the right cache entry.
 */
data class RepoSelection(
    val repoGroup: RepoIssueGroup,
    val language: LanguageFilter,
)
