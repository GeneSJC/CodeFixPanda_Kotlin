package io.github.genesjc.codefixpanda.model

/** Where a token currently in effect came from — shown in Settings so a user can tell which one the app is using. */
enum class TokenSource {
    /** A token the user entered in Settings. */
    USER,

    /** The token baked in at build time from local.properties. */
    BUILD,

    /** Neither is set, so live requests can't be made. */
    NONE,
}
