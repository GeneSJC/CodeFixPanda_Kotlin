package io.github.genesjc.codefixpanda.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/** The real Android Keystore round trip; it needs a device or emulator. */
@RunWith(AndroidJUnit4::class)
class KeystoreTokenCipherTest {

    private val cipher = KeystoreTokenCipher()

    @Test
    fun aTokenSurvivesEncryptingAndDecrypting() {
        val sealed = cipher.encrypt("ghp_secret_token_value")

        assertFalse(sealed.contains("ghp_"))
        assertEquals("ghp_secret_token_value", cipher.decrypt(sealed))
    }

    @Test
    fun theSameTokenSealsDifferentlyEachTime() {
        assertNotEquals(cipher.encrypt("ghp_same"), cipher.encrypt("ghp_same"))
    }

    @Test
    fun textThatIsNotOursDecryptsToNull() {
        assertNull(cipher.decrypt("ghp_plain_text_token"))
        assertNull(cipher.decrypt(""))
        assertNull(cipher.decrypt("AAAA"))
    }
}
