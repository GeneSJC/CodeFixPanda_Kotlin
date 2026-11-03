package io.github.genesjc.opensourcepanda.data

import io.github.genesjc.opensourcepanda.model.TokenStorageProblem

/**
 * Turns the token into text that is safe to write to a file, and back. Split from the storage so the
 * storage's rules can be unit-tested on the plain JVM with a fake; the real one is [KeystoreTokenCipher].
 */
interface TokenCipher {
    fun encrypt(plain: String): String

    /** The original text, or null when [encoded] cannot be decrypted (not ours, damaged, or its key is gone). */
    fun decrypt(encoded: String): String?
}

/**
 * Wraps another [ApiTokenStorage] so that what reaches it is encrypted. A saved value that cannot be
 * decrypted counts as "no token" and is wiped on the spot: that covers a token saved before encryption
 * existed (which would otherwise sit in the file in plain text), a damaged value, and a lost key. If
 * encrypting fails, nothing is written (the token is never stored in plain text) and the token lasts only
 * until the app closes. Either way [lastProblem] says so, so the user can be told and paste the token again.
 */
class EncryptingApiTokenStorage(
    private val delegate: ApiTokenStorage,
    private val cipher: TokenCipher,
) : ApiTokenStorage {

    private var problem: TokenStorageProblem? = null

    override val lastProblem: TokenStorageProblem? get() = problem

    override fun load(): String? {
        val stored = delegate.load() ?: return null
        val plain = cipher.decrypt(stored)
        if (plain == null) {
            delegate.save(null)
            problem = TokenStorageProblem.COULD_NOT_READ
        }
        return plain
    }

    override fun save(token: String?) {
        val sealed = try {
            token?.let(cipher::encrypt)
        } catch (e: Exception) {
            problem = TokenStorageProblem.COULD_NOT_SAVE
            return
        }
        delegate.save(sealed)
        problem = null
    }
}
