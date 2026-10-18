package com.example.githubissuefinder.model

/**
 * Maps onto GitHub's label:"..." search qualifier. [description] is the one source of truth for what
 * each label means, shared by LabelInfoDialog and the gate screen's cards so they cannot disagree.
 * [glyph] is a single plain-text symbol (the codebase's convention for icon-like affordances),
 * rendered in the same white-on-accent text as displayName.
 */
enum class IssueLabelFilter(
    val displayName: String,
    val queryValue: String,
    val glyph: String,
    val description: String,
) {
    GOOD_FIRST_ISSUE(
        displayName = "good first issue",
        queryValue = "good first issue",
        glyph = "✦",
        description = "A label maintainers apply to issues they consider good starting points " +
            "for new contributors — usually well-scoped and not requiring deep familiarity " +
            "with the codebase. A good place for a newer or beginner developer to start.",
    ),
    HELP_WANTED(
        displayName = "help wanted",
        queryValue = "help wanted",
        glyph = "⚑",
        description = "A broader label signaling that maintainers welcome outside help on this " +
            "particular issue — maintainers raising a flag that they could use a hand. Can " +
            "range from simple to fairly involved — less beginner-specific than " +
            "\"good first issue.\"",
    ),
}

/**
 * A small fixed set rather than free-text input, per the "nimble" project
 * convention — covers the languages most relevant to an Android-developer
 * portfolio piece. queryValue is null for ANY, meaning "no language
 * qualifier added to the search query."
 */
enum class LanguageFilter(val displayName: String, val queryValue: String?) {
    ANY("Any", null),
    KOTLIN("Kotlin", "kotlin"),
    JAVA("Java", "java"),
    SWIFT("Swift", "swift"),
    JAVASCRIPT("JavaScript", "javascript"),
    PYTHON("Python", "python"),
}
