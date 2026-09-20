package com.example.githubissuefinder.ui.discover

import com.example.githubissuefinder.model.Issue
import com.example.githubissuefinder.model.IssueLabelFilter

data class RepoIssuesUiState(
    val issues: List<Issue> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedLabel: IssueLabelFilter = IssueLabelFilter.GOOD_FIRST_ISSUE,
    /**
     * True once a load for HELP_WANTED has come back with zero issues
     * this session (v0.40) — learned live, since (unlike good first
     * issue) there's no pre-fetched count for this label. Sticky for
     * the rest of this screen's session once set; never reset back to
     * false even if a later load somehow finds issues, since the
     * underlying issue set isn't expected to fluctuate within one
     * session. A deliberate, modest simplification, not re-verified
     * on every load.
     */
    val helpWantedKnownEmpty: Boolean = false,
)
