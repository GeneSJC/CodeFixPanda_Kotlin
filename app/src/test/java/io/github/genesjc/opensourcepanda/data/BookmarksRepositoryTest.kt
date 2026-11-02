package io.github.genesjc.opensourcepanda.data

import io.github.genesjc.opensourcepanda.model.Issue
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookmarksRepositoryTest {

    private class FakeStorage(var stored: List<Issue> = emptyList()) : BookmarksStorage {
        var failSaves = false
        var saveCount = 0
        override fun load(): List<Issue> = stored
        override fun save(issues: List<Issue>, onFailure: () -> Unit) {
            if (failSaves) {
                onFailure()
                return
            }
            stored = issues
            saveCount++
        }
    }

    private fun issue(id: Long, title: String = "Issue $id") = Issue(
        id = id,
        title = title,
        bodyText = "",
        repoOwner = "octo",
        repoName = "repo",
        labels = emptyList(),
        language = null,
        commentCount = 0,
        updatedAtMs = null,
        htmlUrl = "https://github.com/octo/repo/issues/$id",
    )

    @Test
    fun aFailedSaveIsReportedAndTheChangeStaysInMemory() = runBlocking {
        val storage = FakeStorage().apply { failSaves = true }
        val repository = BookmarksRepository(storage)
        val reported = async(start = CoroutineStart.UNDISPATCHED) { repository.saveFailures.first() }

        repository.toggle(issue(1))

        withTimeout(1_000) { reported.await() }
        assertTrue(1L in repository.bookmarkedIds.value)
    }

    @Test
    fun startsEmptyWhenNothingIsSaved() {
        val repository = BookmarksRepository(FakeStorage())
        assertTrue(repository.bookmarkedIds.value.isEmpty())
        assertTrue(repository.bookmarks.value.isEmpty())
    }

    @Test
    fun restoresSavedBookmarksInOrderOnConstruction() {
        val repository = BookmarksRepository(FakeStorage(listOf(issue(2), issue(1))))
        assertEquals(setOf(1L, 2L), repository.bookmarkedIds.value)
        assertEquals(listOf(2L, 1L), repository.bookmarks.value.map { it.id })
    }

    @Test
    fun togglingAnUnbookmarkedIssueBookmarksItAndReturnsTrue() {
        val repository = BookmarksRepository(FakeStorage())

        val nowBookmarked = repository.toggle(issue(5, "Five"))

        assertTrue(nowBookmarked)
        assertEquals(setOf(5L), repository.bookmarkedIds.value)
        assertEquals("Five", repository.bookmarks.value.single().title)
    }

    @Test
    fun togglingABookmarkedIssueRemovesItAndReturnsFalse() {
        val repository = BookmarksRepository(FakeStorage())
        repository.toggle(issue(5))

        val nowBookmarked = repository.toggle(issue(5))

        assertFalse(nowBookmarked)
        assertTrue(repository.bookmarkedIds.value.isEmpty())
        assertTrue(repository.bookmarks.value.isEmpty())
    }

    @Test
    fun bookmarksAreListedInTheOrderTheyWereSaved() {
        val repository = BookmarksRepository(FakeStorage())
        repository.toggle(issue(3))
        repository.toggle(issue(1))
        repository.toggle(issue(2))
        assertEquals(listOf(3L, 1L, 2L), repository.bookmarks.value.map { it.id })
    }

    @Test
    fun removingOneLeavesTheOthersInOrder() {
        val repository = BookmarksRepository(FakeStorage())
        repository.toggle(issue(3))
        repository.toggle(issue(1))
        repository.toggle(issue(2))

        repository.toggle(issue(1))

        assertEquals(listOf(3L, 2L), repository.bookmarks.value.map { it.id })
    }

    @Test
    fun everyToggleIsPersisted() {
        val storage = FakeStorage()
        val repository = BookmarksRepository(storage)

        repository.toggle(issue(1))
        assertEquals(listOf(1L), storage.stored.map { it.id })

        repository.toggle(issue(1))
        assertTrue(storage.stored.isEmpty())
        assertEquals(2, storage.saveCount)
    }

    /** A new repository over the same storage stands in for killing and relaunching the app. */
    @Test
    fun bookmarksSurviveARestart() {
        val storage = FakeStorage()
        BookmarksRepository(storage).toggle(issue(8, "Eight"))

        val afterRestart = BookmarksRepository(storage)

        assertEquals(setOf(8L), afterRestart.bookmarkedIds.value)
        assertEquals("Eight", afterRestart.bookmarks.value.single().title)
    }

    @Test
    fun togglingDifferentIssuesDoesNotAffectEachOther() {
        val repository = BookmarksRepository(FakeStorage())
        repository.toggle(issue(1))
        repository.toggle(issue(2))
        repository.toggle(issue(1))
        assertEquals(setOf(2L), repository.bookmarkedIds.value)
    }
}
