package com.example.githubissuefinder.ui.discover

import com.example.githubissuefinder.model.Issue
import com.example.githubissuefinder.model.IssueLabelFilter

data class RepoIssuesUiState(
    val issues: List<Issue> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedLabel: IssueLabelFilter = IssueLabelFilter.GOOD_FIRST_ISSUE,
    /**
     * True once a load for whichever label ISN'T the repo's own
     * pre-fetched mode has come back with zero issues this session —
     * renamed from helpWantedKnownEmpty (v0.41), which hardcoded the
     * assumption that "the other label" always meant specifically
     * Help Wanted. Once Issue Mode meant the pre-fetched label could
     * just as easily BE Help Wanted (with Good First Issue as "the
     * other one"), that hardcoding stopped being safe. Learned live,
     * since (unlike the repo's own active-mode count) there's no
     * pre-fetched count for whichever label this is. Sticky for the
     * rest of this screen's session once set; never reset back to
     * false even if a later load somehow finds issues, since the
     * underlying issue set isn't expected to fluctuate within one
     * session. A deliberate, modest simplification, not re-verified
     * on every load — unchanged from before this rename.
     */
    val otherLabelKnownEmpty: Boolean = false,
)
