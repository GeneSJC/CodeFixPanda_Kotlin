package com.example.githubissuefinder.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
