package io.github.genesjc.codefixpanda.model

/**
 * A small fixed set of the languages most relevant to Android developers, not free-text input.
 * queryValue is null for ANY, meaning no language qualifier is added to the search query.
 */
enum class LanguageFilter(val queryValue: String?) {
    ANY(null),
    KOTLIN("kotlin"),
    JAVA("java"),
    SWIFT("swift"),
    JAVASCRIPT("javascript"),
    PYTHON("python"),
}
