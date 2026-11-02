package io.github.genesjc.opensourcepanda.data

import io.github.genesjc.opensourcepanda.model.DEMO_TAG
import io.github.genesjc.opensourcepanda.model.HiddenIssue
import io.github.genesjc.opensourcepanda.model.Issue
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HiddenIssuesRepositoryTest {

    private class FakeStorage(var stored: List<HiddenIssue> = emptyList()) : HiddenIssuesStorage {
        var failSaves = false
        var saveCount = 0
        override fun load(): List<HiddenIssue> = stored
        override fun save(issues: List<HiddenIssue>, onFailure: () -> Unit) {
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

    private fun repository(storage: FakeStorage = FakeStorage(), now: () -> Long = { 1_000L }) =
        HiddenIssuesRepository(storage, now)

    @Test
    fun aFailedSaveIsReportedAndTheChangeStaysInMemory() = runBlocking {
        val repository = repository(FakeStorage().apply { failSaves = true })
        val reported = async(start = CoroutineStart.UNDISPATCHED) { repository.saveFailures.first() }

        repository.hide(issue(1))

        withTimeout(1_000) { reported.await() }
        assertTrue(1L in repository.hiddenIds.value)
    }

    @Test
    fun startsEmptyWhenNothingIsSaved() {
        val repository = repository()
        assertTrue(repository.hiddenIds.value.isEmpty())
        assertTrue(repository.hiddenIssues.value.isEmpty())
    }

    @Test
    fun restoresSavedHiddenIssuesOnConstruction() {
        val saved = listOf(HiddenIssue(7, "Seven", "octo/repo", "https://x/7", 500L))
        val repository = repository(FakeStorage(saved))
        assertEquals(setOf(7L), repository.hiddenIds.value)
        assertEquals(saved, repository.hiddenIssues.value)
    }

    @Test
    fun hidingRecordsASnapshotAndAddsItsId() {
        val repository = repository(now = { 4_242L })

        repository.hide(issue(3, "Three"))

        assertEquals(setOf(3L), repository.hiddenIds.value)
        val hidden = repository.hiddenIssues.value.single()
        assertEquals(3L, hidden.id)
        assertEquals("Three", hidden.title)
        assertEquals("octo/repo", hidden.repoFullName)
        assertEquals("https://github.com/octo/repo/issues/3", hidden.htmlUrl)
        assertEquals(4_242L, hidden.hiddenAtMs)
    }

    @Test
    fun hidingPersistsToStorage() {
        val storage = FakeStorage()
        repository(storage).hide(issue(3))
        assertEquals(listOf(3L), storage.stored.map { it.id })
    }

    @Test
    fun hiddenIssuesAreListedNewestFirst() {
        var time = 100L
        val repository = repository(now = { time })

        repository.hide(issue(1)); time = 300L
        repository.hide(issue(2)); time = 200L
        repository.hide(issue(3))

        assertEquals(listOf(2L, 3L, 1L), repository.hiddenIssues.value.map { it.id })
    }

    @Test
    fun hidingTheSameIssueTwiceKeepsOneEntry() {
        val repository = repository()
        repository.hide(issue(5))
        repository.hide(issue(5))
        assertEquals(1, repository.hiddenIssues.value.size)
        assertEquals(setOf(5L), repository.hiddenIds.value)
    }

    @Test
    fun unhidingRemovesTheIssueAndPersists() {
        val storage = FakeStorage()
        val repository = repository(storage)
        repository.hide(issue(1))
        repository.hide(issue(2))

        repository.unhide(1)

        assertEquals(setOf(2L), repository.hiddenIds.value)
        assertEquals(listOf(2L), repository.hiddenIssues.value.map { it.id })
        assertEquals(listOf(2L), storage.stored.map { it.id })
    }

    @Test
    fun unhidingSomethingNotHiddenDoesNothingAndDoesNotWrite() {
        val storage = FakeStorage()
        val repository = repository(storage)
        repository.hide(issue(1))
        val writesBefore = storage.saveCount

        repository.unhide(99)

        assertEquals(setOf(1L), repository.hiddenIds.value)
        assertEquals(writesBefore, storage.saveCount)
    }

    @Test
    fun unhideAllClearsEverythingAndPersists() {
        val storage = FakeStorage()
        val repository = repository(storage)
        repository.hide(issue(1))
        repository.hide(issue(2))

        repository.unhideAll(tag = null)

        assertTrue(repository.hiddenIds.value.isEmpty())
        assertTrue(repository.hiddenIssues.value.isEmpty())
        assertTrue(storage.stored.isEmpty())
    }

    @Test
    fun unhideAllWhenNothingIsHiddenDoesNotWrite() {
        val storage = FakeStorage()
        repository(storage).unhideAll(tag = null)
        assertEquals(0, storage.saveCount)
    }

    @Test
    fun hidingKeepsTheIssuesTag() {
        val repository = repository(FakeStorage())
        repository.hide(issue(1).copy(tag = DEMO_TAG))

        assertEquals(DEMO_TAG, repository.hiddenIssues.value.single().tag)
    }

    @Test
    fun unhideAllLeavesEntriesWithAnotherTagAlone() {
        val repository = repository(FakeStorage())
        repository.hide(issue(1))
        repository.hide(issue(2).copy(tag = DEMO_TAG))

        repository.unhideAll(tag = null)

        assertEquals(listOf(2L), repository.hiddenIssues.value.map { it.id })
    }

    /** A new repository over the same storage stands in for killing and relaunching the app. */
    @Test
    fun hiddenIssuesSurviveARestart() {
        val storage = FakeStorage()
        repository(storage).hide(issue(8, "Eight"))

        val afterRestart = repository(storage)

        assertEquals(setOf(8L), afterRestart.hiddenIds.value)
        assertEquals("Eight", afterRestart.hiddenIssues.value.single().title)
    }
}
