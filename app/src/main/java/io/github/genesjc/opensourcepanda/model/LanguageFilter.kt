package io.github.genesjc.opensourcepanda.model

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
