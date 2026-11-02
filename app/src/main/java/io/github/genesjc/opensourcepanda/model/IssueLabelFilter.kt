package io.github.genesjc.opensourcepanda.model

/**
 * Maps onto GitHub's label:"..." search qualifier. The wording the user reads for each mode (its
 * name and what it means) lives in string resources, reached through IssueFilterText.kt, so this
 * type holds only facts.
 */
enum class IssueLabelFilter(
    val queryValue: String,
) {
    GOOD_FIRST_ISSUE(queryValue = "good first issue"),
    HELP_WANTED(queryValue = "help wanted"),
}
