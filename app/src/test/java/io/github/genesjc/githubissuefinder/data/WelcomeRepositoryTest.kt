package io.github.genesjc.githubissuefinder.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WelcomeRepositoryTest {

    private class FakeStorage(var seen: Boolean = false) : WelcomeStorage {
        var markCount = 0
        override fun hasSeenWelcome(): Boolean = seen
        override fun markWelcomeSeen() {
            seen = true
            markCount++
        }
    }

    @Test
    fun hasNotSeenTheWelcomeOnAFreshInstall() {
        assertFalse(WelcomeRepository(FakeStorage()).hasSeenWelcome)
    }

    @Test
    fun reportsSeenOnceMarkedAndPersistsIt() {
        val storage = FakeStorage()
        val repository = WelcomeRepository(storage)

        repository.markWelcomeSeen()

        assertTrue(repository.hasSeenWelcome)
        assertTrue(storage.seen)
        assertEquals(1, storage.markCount)
    }

    /** A new repository over the same storage stands in for killing and relaunching the app. */
    @Test
    fun aNewRepositoryOverTheSameStorageStillRemembers() {
        val storage = FakeStorage()
        WelcomeRepository(storage).markWelcomeSeen()

        assertTrue(WelcomeRepository(storage).hasSeenWelcome)
    }

    @Test
    fun theWelcomeShowsOnlyWithNoTokenAndNotYetSeen() {
        assertTrue(needsWelcome(hasToken = false, welcomeSeen = false))
        assertFalse(needsWelcome(hasToken = false, welcomeSeen = true))
        assertFalse(needsWelcome(hasToken = true, welcomeSeen = false))
        assertFalse(needsWelcome(hasToken = true, welcomeSeen = true))
    }
}
