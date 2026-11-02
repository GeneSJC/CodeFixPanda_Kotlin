package io.github.genesjc.opensourcepanda.data

import io.github.genesjc.opensourcepanda.model.TokenStorageProblem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class EncryptingApiTokenStorageTest {

    private class FakeStorage(var stored: String? = null) : ApiTokenStorage {
        override fun load(): String? = stored
        override fun save(token: String?) {
            stored = token
        }
    }

    /** "Encrypts" by adding a marker, so a test can tell sealed text from plain text. */
    private class FakeCipher(var failToEncrypt: Boolean = false) : TokenCipher {
        override fun encrypt(plain: String): String {
            if (failToEncrypt) error("no key")
            return "sealed:$plain"
        }

        override fun decrypt(encoded: String): String? = encoded.removePrefix("sealed:").takeIf { encoded.startsWith("sealed:") }
    }

    private val inner = FakeStorage()
    private val cipher = FakeCipher()
    private val storage = EncryptingApiTokenStorage(inner, cipher)

    @Test
    fun whatReachesTheFileIsEncryptedNotPlain() {
        storage.save("ghp_secret")

        assertEquals("sealed:ghp_secret", inner.stored)
        assertFalse(inner.stored!!.startsWith("ghp_"))
    }

    @Test
    fun loadingGivesBackTheOriginalToken() {
        storage.save("ghp_secret")

        assertEquals("ghp_secret", storage.load())
    }

    @Test
    fun savingNullClearsTheStoredValue() {
        storage.save("ghp_secret")

        storage.save(null)

        assertNull(inner.stored)
        assertNull(storage.load())
    }

    @Test
    fun aTokenThatCannotBeDecryptedIsDiscardedAndReported() {
        inner.stored = "not-ours"

        assertNull(storage.load())

        assertNull(inner.stored)
        assertEquals(TokenStorageProblem.COULD_NOT_READ, storage.lastProblem)
    }

    @Test
    fun aTokenThatCannotBeEncryptedIsNotStoredAndIsReported() {
        cipher.failToEncrypt = true

        storage.save("ghp_secret")

        assertNull(inner.stored)
        assertEquals(TokenStorageProblem.COULD_NOT_SAVE, storage.lastProblem)
    }

    @Test
    fun aSuccessfulSaveClearsTheProblem() {
        cipher.failToEncrypt = true
        storage.save("ghp_secret")
        cipher.failToEncrypt = false

        storage.save("ghp_secret")

        assertNull(storage.lastProblem)
    }

    @Test
    fun nothingStoredLoadsAsNoToken() {
        assertNull(storage.load())
    }

    @Test
    fun aPlainTextTokenFromBeforeEncryptionLoadsAsNoTokenAndIsWiped() {
        inner.stored = "ghp_old_plain_token"

        assertNull(storage.load())
        assertNull(inner.stored)
    }

    @Test
    fun ifEncryptingFailsNothingIsWrittenAndNothingIsThrown() {
        cipher.failToEncrypt = true

        storage.save("ghp_secret")

        assertNull(inner.stored)
    }
}
