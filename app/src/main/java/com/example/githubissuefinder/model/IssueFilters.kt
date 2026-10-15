package com.example.githubissuefinder.model

/**
 * Maps directly onto GitHub's label:"..." search qualifier.
 *
 * glyph/description added alongside IssueModeGateScreen's expanding
 * mode cards (per direct instruction, after a "good first issue" wasn't
 * self-explanatory to a user who isn't deep in GitHub/OSS convention) —
 * description is the exact wording LabelInfoDialog already showed for
 * each label, moved here as the one source of truth so that dialog and
 * the gate screen's cards can't drift apart saying two different things
 * about the same label. glyph is a single plain-text symbol (this
 * codebase's existing convention for every icon-like affordance — see
 * e.g. RepoIssuesScreen's "←"/"↗" — not an icon-library drawable),
 * rendered in the same white-on-accent-color text used for displayName
 * itself so it stays genuinely monochromatic per that request, rather
 * than a colorful emoji.
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
