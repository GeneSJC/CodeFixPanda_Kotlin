package io.github.genesjc.opensourcepanda.ui.toprepos

/** The dialogs Top Repos can show, one at a time. */
internal enum class TopReposDialog {
    /** What the repo count means (opened from the count chip). */
    RANKING_INFO,

    /** What the two issue types mean (opened from the drawer). */
    LABEL_INFO,

    /** The Search API's rate-limit numbers. */
    RATE_LIMIT,

    /** Why live data is not loading: no token, or one GitHub rejected. */
    TOKEN_PROBLEM,

    /** The confirmation before the whole cache is cleared. */
    CLEAR_CACHE,
}
