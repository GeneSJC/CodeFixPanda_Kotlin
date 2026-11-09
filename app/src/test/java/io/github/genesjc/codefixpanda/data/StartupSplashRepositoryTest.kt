package io.github.genesjc.codefixpanda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupSplashRepositoryTest {

    private class FakeStorage(var seen: Boolean = false) : StartupSplashStorage {
        var markCount = 0
        override fun hasSeenFullAnimation(): Boolean = seen
        override fun markFullAnimationSeen() {
            seen = true
            markCount++
        }
    }

    @Test
    fun hasNotSeenTheAnimationOnAFreshInstall() {
        assertFalse(StartupSplashRepository(FakeStorage()).hasSeenFullAnimation)
    }

    @Test
    fun reportsSeenOnceMarked() {
        val repository = StartupSplashRepository(FakeStorage())
        repository.markFullAnimationSeen()
        assertTrue(repository.hasSeenFullAnimation)
    }

    @Test
    fun markingIsPersistedToStorage() {
        val storage = FakeStorage()
        StartupSplashRepository(storage).markFullAnimationSeen()
        assertTrue(storage.seen)
    }

    /** A new repository over the same storage stands in for killing and relaunching the app. */
    @Test
    fun aNewRepositoryOverTheSameStorageStillRemembers() {
        val storage = FakeStorage()
        StartupSplashRepository(storage).markFullAnimationSeen()
        assertTrue(StartupSplashRepository(storage).hasSeenFullAnimation)
    }

    @Test
    fun markingTwiceStaysSeen() {
        val storage = FakeStorage()
        val repository = StartupSplashRepository(storage)
        repository.markFullAnimationSeen()
        repository.markFullAnimationSeen()
        assertTrue(repository.hasSeenFullAnimation)
        assertEquals(2, storage.markCount)
    }
}
