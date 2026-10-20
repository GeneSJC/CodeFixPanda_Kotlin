package io.github.genesjc.githubissuefinder.ui

import androidx.annotation.StringRes
import io.github.genesjc.githubissuefinder.R
import io.github.genesjc.githubissuefinder.model.IssueLabelFilter
import io.github.genesjc.githubissuefinder.model.LanguageFilter

/**
 * The wording for the filter types, as string resource ids. The `when`s are exhaustive, so adding a
 * mode or a language will not compile until its text is added here.
 */

/** The mode's name as used in a sentence or a pill: "good first issue". */
@StringRes
fun IssueLabelFilter.nameRes(): Int = when (this) {
    IssueLabelFilter.GOOD_FIRST_ISSUE -> R.string.mode_good_first_issue_name
    IssueLabelFilter.HELP_WANTED -> R.string.mode_help_wanted_name
}

/** The mode's name as a button caption: "Good First Issue". */
@StringRes
fun IssueLabelFilter.buttonLabelRes(): Int = when (this) {
    IssueLabelFilter.GOOD_FIRST_ISSUE -> R.string.mode_good_first_issue_button
    IssueLabelFilter.HELP_WANTED -> R.string.mode_help_wanted_button
}

/** What the mode's GitHub label means, shared by the info dialog and the gate screen's cards. */
@StringRes
fun IssueLabelFilter.descriptionRes(): Int = when (this) {
    IssueLabelFilter.GOOD_FIRST_ISSUE -> R.string.mode_good_first_issue_description
    IssueLabelFilter.HELP_WANTED -> R.string.mode_help_wanted_description
}

/** The language chip's label. */
@StringRes
fun LanguageFilter.nameRes(): Int = when (this) {
    LanguageFilter.ANY -> R.string.language_any
    LanguageFilter.KOTLIN -> R.string.language_kotlin
    LanguageFilter.JAVA -> R.string.language_java
    LanguageFilter.SWIFT -> R.string.language_swift
    LanguageFilter.JAVASCRIPT -> R.string.language_javascript
    LanguageFilter.PYTHON -> R.string.language_python
}
