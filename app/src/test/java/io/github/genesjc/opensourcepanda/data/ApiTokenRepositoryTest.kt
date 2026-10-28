package io.github.genesjc.opensourcepanda.data

import io.github.genesjc.opensourcepanda.model.DEMO_TAG
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiTokenRepositoryTest {

    private class FakeStorage(var stored: String? = null) : ApiTokenStorage {
        override fun load(): String? = stored
        override fun save(token: String?) {
            stored = token
        }
    }

    // ---- the precedence rule, as pure functions ----

    @Test
    fun userTokenBeatsTheBuildToken() {
        assertEquals("user", resolveEffectiveToken("user", "build"))
    }

    @Test
    fun buildTokenIsUsedWhenThereIsNoUserToken() {
        assertEquals("build", resolveEffectiveToken("", "build"))
    }

    @Test
    fun blankUserTokenFallsBackToTheBuildToken() {
        assertEquals("build", resolveEffectiveToken("   ", "build"))
    }

    @Test
    fun noTokenAtAllResolvesToEmpty() {
        assertEquals("", resolveEffectiveToken("", ""))
        assertEquals("", resolveEffectiveToken("  ", "  "))
    }

    @Test
    fun tokenSourceFollowsTheSameRule() {
        assertEquals(TokenSource.USER, tokenSourceOf("user", "build"))
        assertEquals(TokenSource.USER, tokenSourceOf("user", ""))
        assertEquals(TokenSource.BUILD, tokenSourceOf("", "build"))
        assertEquals(TokenSource.BUILD, tokenSourceOf("  ", "build"))
        assertEquals(TokenSource.NONE, tokenSourceOf("", ""))
    }

    // ---- the repository ----

    @Test
    fun startsWithTheBuildTokenWhenNothingIsSaved() {
        val repository = ApiTokenRepository(FakeStorage(), buildToken = "build")
        assertEquals("", repository.userToken.value)
        assertEquals("build", repository.effectiveToken.value)
        assertEquals(TokenSource.BUILD, repository.tokenSource.value)
    }

    @Test
    fun startsWithNoTokenWhenNeitherExists() {
        val repository = ApiTokenRepository(FakeStorage(), buildToken = "")
        assertEquals("", repository.effectiveToken.value)
        assertEquals(TokenSource.NONE, repository.tokenSource.value)
    }

    @Test
    fun restoresASavedUserTokenOnConstruction() {
        val repository = ApiTokenRepository(FakeStorage("saved"), buildToken = "build")
        assertEquals("saved", repository.userToken.value)
        assertEquals("saved", repository.effectiveToken.value)
        assertEquals(TokenSource.USER, repository.tokenSource.value)
    }

    @Test
    fun savingATokenOverridesTheBuildTokenAndPersists() {
        val storage = FakeStorage()
        val repository = ApiTokenRepository(storage, buildToken = "build")

        repository.setUserToken("mine")

        assertEquals("mine", repository.effectiveToken.value)
        assertEquals(TokenSource.USER, repository.tokenSource.value)
        assertEquals("mine", storage.stored)
    }

    @Test
    fun savingTrimsSurroundingWhitespace() {
        val storage = FakeStorage()
        val repository = ApiTokenRepository(storage, buildToken = "")

        repository.setUserToken("  ghp_abc123\n")

        assertEquals("ghp_abc123", repository.userToken.value)
        assertEquals("ghp_abc123", repository.effectiveToken.value)
        assertEquals("ghp_abc123", storage.stored)
    }

    @Test
    fun clearingFallsBackToTheBuildTokenAndClearsStorage() {
        val storage = FakeStorage()
        val repository = ApiTokenRepository(storage, buildToken = "build")
        repository.setUserToken("mine")

        repository.setUserToken("")

        assertEquals("", repository.userToken.value)
        assertEquals("build", repository.effectiveToken.value)
        assertEquals(TokenSource.BUILD, repository.tokenSource.value)
        assertNull(storage.stored)
    }

    @Test
    fun savingABlankValueCountsAsClearing() {
        val storage = FakeStorage("old")
        val repository = ApiTokenRepository(storage, buildToken = "")

        repository.setUserToken("   ")

        assertEquals(TokenSource.NONE, repository.tokenSource.value)
        assertNull(storage.stored)
    }

    /** A new repository over the same storage stands in for killing and relaunching the app. */
    @Test
    fun aSavedTokenSurvivesARestart() {
        val storage = FakeStorage()
        ApiTokenRepository(storage, buildToken = "build").setUserToken("mine")

        val afterRestart = ApiTokenRepository(storage, buildToken = "build")

        assertEquals("mine", afterRestart.effectiveToken.value)
    }

    @Test
    fun replacingATokenKeepsOnlyTheLatest() {
        val repository = ApiTokenRepository(FakeStorage(), buildToken = "")
        repository.setUserToken("first")
        repository.setUserToken("second")
        assertEquals("second", repository.effectiveToken.value)
    }

    // ---- which data is in use ----

    @Test
    fun noTokenMeansTheDemoTag() {
        assertEquals(DEMO_TAG, activeTagFor(""))
        assertEquals(DEMO_TAG, ApiTokenRepository(FakeStorage(), buildToken = "").activeTag.value)
    }

    @Test
    fun aTokenMeansNoTag() {
        assertNull(activeTagFor("abc"))
        assertNull(ApiTokenRepository(FakeStorage("user"), buildToken = "").activeTag.value)
        assertNull(ApiTokenRepository(FakeStorage(), buildToken = "build").activeTag.value)
    }

    @Test
    fun isDemoIsTrueExactlyWhileThereIsNoToken() {
        val repository = ApiTokenRepository(FakeStorage(), buildToken = "")
        assertTrue(repository.isDemo.value)

        repository.setUserToken("abc")
        assertFalse(repository.isDemo.value)

        repository.setUserToken("")
        assertTrue(repository.isDemo.value)
    }

    @Test
    fun theTagFollowsTheTokenAsItIsSetAndCleared() {
        val repository = ApiTokenRepository(FakeStorage(), buildToken = "")

        repository.setUserToken("abc")
        assertNull(repository.activeTag.value)

        repository.setUserToken("")
        assertEquals(DEMO_TAG, repository.activeTag.value)
    }

    // ---- a demo started for the session ----

    @Test
    fun startingTheDemoShowsDemoDataEvenWithATokenAndChangesNothingStored() {
        val storage = FakeStorage("ghp_saved")
        val repository = ApiTokenRepository(storage, buildToken = "build")

        repository.startDemo()

        assertTrue(repository.isDemo.value)
        assertEquals("", repository.effectiveToken.value)
        assertEquals(TokenSource.NONE, repository.tokenSource.value)
        assertEquals("ghp_saved", repository.userToken.value)
        assertEquals("ghp_saved", storage.stored)
    }

    @Test
    fun savingATokenEndsTheDemo() {
        val repository = ApiTokenRepository(FakeStorage(), buildToken = "")
        repository.startDemo()

        repository.setUserToken("ghp_new")

        assertFalse(repository.isDemo.value)
        assertEquals("ghp_new", repository.effectiveToken.value)
        assertEquals(TokenSource.USER, repository.tokenSource.value)
    }

    @Test
    fun theDemoLastsOnlyForTheSession() {
        val storage = FakeStorage("ghp_saved")
        ApiTokenRepository(storage, buildToken = "").startDemo()

        val nextLaunch = ApiTokenRepository(storage, buildToken = "")

        assertFalse(nextLaunch.isDemo.value)
        assertEquals("ghp_saved", nextLaunch.effectiveToken.value)
    }
}
