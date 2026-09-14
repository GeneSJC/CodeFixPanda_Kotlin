package com.example.githubissuefinder.model

/** Maps directly onto GitHub's label:"..." search qualifier. */
enum class IssueLabelFilter(val displayName: String, val queryValue: String) {
    GOOD_FIRST_ISSUE("good first issue", "good first issue"),
    HELP_WANTED("help wanted", "help wanted"),
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
