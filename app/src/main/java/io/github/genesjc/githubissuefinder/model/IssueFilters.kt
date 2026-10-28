package io.github.genesjc.githubissuefinder.model

/**
 * Maps onto GitHub's label:"..." search qualifier. The wording the user reads for each mode (its
 * name and what it means) lives in string resources, reached through IssueFilterText.kt, so this
 * type holds only facts. [glyph] is a single plain-text symbol (the codebase's convention for
 * icon-like affordances), rendered in the same white-on-accent text as the mode's name.
 */
enum class IssueLabelFilter(
    val queryValue: String,
    val glyph: String,
) {
    GOOD_FIRST_ISSUE(queryValue = "good first issue", glyph = "✦"),
    HELP_WANTED(queryValue = "help wanted", glyph = "⚑"),
}

/**
 * A small fixed set rather than free-text input, per the "nimble" project
 * convention — covers the languages most relevant to Android developers.
 * queryValue is null for ANY, meaning "no language
 * qualifier added to the search query."
 */
enum class LanguageFilter(val queryValue: String?) {
    ANY(null),
    KOTLIN("kotlin"),
    JAVA("java"),
    SWIFT("swift"),
    JAVASCRIPT("javascript"),
    PYTHON("python"),
}
