package io.github.genesjc.codefixpanda.model

/** Something that went wrong keeping the saved GitHub token, which the user should be told about. */
enum class TokenStorageProblem {
    /** A saved token could not be decrypted (for example after a phone restore), so it was discarded. */
    COULD_NOT_READ,

    /** The token could not be encrypted, so it was not saved and lasts only until the app closes. */
    COULD_NOT_SAVE,
}
