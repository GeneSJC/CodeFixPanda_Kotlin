package io.github.genesjc.codefixpanda.model

/**
 * Where the data on screen comes from: GitHub (the user has a token), or the built-in demo data (they do
 * not). Every issue records its origin, and it is saved with bookmarks and hidden issues, so demo data and
 * real data never appear together: a screen that lists saved issues shows only those whose origin matches
 * the one in use.
 */
enum class DataOrigin {
    GITHUB,
    DEMO,
}
