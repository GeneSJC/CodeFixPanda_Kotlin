package io.github.genesjc.codefixpanda.data

import io.github.genesjc.codefixpanda.model.Issue
import io.github.genesjc.codefixpanda.model.IssueLabelFilter
import io.github.genesjc.codefixpanda.model.LanguageFilter
import io.github.genesjc.codefixpanda.model.RepoIssueGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepoIssueGroupRepositoryTest {

    private class FakeStorage : RepoIssueGroupStorage {
        var data: Map<String, RepoIssueGroupCacheEntry> = emptyMap()
        var writes = 0
        var clears = 0
        override fun readAll() = data
        override fun writeAll(all: Map<String, RepoIssueGroupCacheEntry>) {
            data = all
            writes++
        }

        override fun clear() {
            data = emptyMap()
            clears++
        }
    }

    private fun issue(id: Long, owner: String = "octo", repo: String = "repo") = Issue(
        id = id, title = "Issue $id", bodyText = "", repoOwner = owner, repoName = repo, labels = emptyList(),
        language = null, commentCount = 0, updatedAtMs = null,
        htmlUrl = "https://github.com/$owner/$repo/issues/$id",
    )

    private fun group(vararg issues: Issue) = RepoIssueGroup(
        repoOwner = issues.first().repoOwner,
        repoName = issues.first().repoName,
        repoFullName = issues.first().repoFullName,
        repoDisplayTitle = issues.first().repoDisplayTitle,
        issues = issues.toList(),
    )

    private fun repository(storage: FakeStorage = FakeStorage(), now: () -> Long = { 1_000L }) =
        RepoIssueGroupRepository(storage, Dispatchers.Unconfined, now)

    private val kotlin = LanguageFilter.KOTLIN
    private val gfi = IssueLabelFilter.GOOD_FIRST_ISSUE
    private val help = IssueLabelFilter.HELP_WANTED

    @Test
    fun loadingAnUnsavedCombinationReturnsNull() = runBlocking {
        assertNull(repository().load(kotlin, gfi))
    }

    @Test
    fun aSavedEntryComesBackWithEverythingItWasSavedWith() = runBlocking {
        val repository = repository(now = { 4_242L })
        val groups = listOf(group(issue(1), issue(2)))

        repository.save(kotlin, gfi, groups, totalIssueCount = 57, currentPage = 3, canLoadMore = false)

        val entry = repository.load(kotlin, gfi)!!
        assertEquals(groups, entry.repoGroups)
        assertEquals(57, entry.totalIssueCount)
        assertEquals(3, entry.currentPage)
        assertEquals(false, entry.canLoadMore)
        assertEquals(4_242L, entry.fetchedAtMs)
    }

    @Test
    fun anExplicitFetchedAtOverridesTheClock() = runBlocking {
        val repository = repository(now = { 1L })
        repository.save(kotlin, gfi, emptyList(), 0, 1, true, fetchedAtMs = 99L)
        assertEquals(99L, repository.load(kotlin, gfi)!!.fetchedAtMs)
    }

    @Test
    fun eachLanguageAndModeCombinationIsKeptSeparately() = runBlocking {
        val repository = repository()
        repository.save(kotlin, gfi, listOf(group(issue(1))), 1, 1, true)
        repository.save(kotlin, help, listOf(group(issue(2))), 2, 1, true)
        repository.save(LanguageFilter.ANY, gfi, listOf(group(issue(3))), 3, 1, true)

        assertEquals(1, repository.load(kotlin, gfi)!!.totalIssueCount)
        assertEquals(2, repository.load(kotlin, help)!!.totalIssueCount)
        assertEquals(3, repository.load(LanguageFilter.ANY, gfi)!!.totalIssueCount)
    }

    @Test
    fun savingAgainReplacesThatCombinationAndLeavesTheOthers() = runBlocking {
        val repository = repository()
        repository.save(kotlin, gfi, listOf(group(issue(1))), 1, 1, true)
        repository.save(kotlin, help, listOf(group(issue(2))), 2, 1, true)

        repository.save(kotlin, gfi, listOf(group(issue(9))), 9, 2, false)

        assertEquals(9, repository.load(kotlin, gfi)!!.totalIssueCount)
        assertEquals(2, repository.load(kotlin, help)!!.totalIssueCount)
    }

    @Test
    fun replacingARepoGroupUpdatesItAndKeepsTheOriginalFetchedAt() = runBlocking {
        var time = 100L
        val repository = repository(now = { time })
        repository.save(kotlin, gfi, listOf(group(issue(1)), group(issue(2, "a", "b"))), 2, 1, true)
        time = 500L

        val refreshed = group(issue(1), issue(3))
        repository.replaceRepoGroup(kotlin, gfi, refreshed.repoFullName, refreshed)

        val entry = repository.load(kotlin, gfi)!!
        assertEquals(100L, entry.fetchedAtMs)
        assertEquals(2, entry.repoGroups.first { it.repoFullName == refreshed.repoFullName }.issues.size)
        assertEquals(2, entry.repoGroups.size)
    }

    @Test
    fun replacingARepoGroupWithNullRemovesTheRepo() = runBlocking {
        val repository = repository()
        val keep = group(issue(2, "a", "b"))
        val drop = group(issue(1))
        repository.save(kotlin, gfi, listOf(drop, keep), 2, 1, true)

        repository.replaceRepoGroup(kotlin, gfi, drop.repoFullName, null)

        assertEquals(listOf(keep), repository.load(kotlin, gfi)!!.repoGroups)
    }

    @Test
    fun replacingWhenNothingIsCachedDoesNothingAndDoesNotWrite() = runBlocking {
        val storage = FakeStorage()
        val repository = repository(storage)

        repository.replaceRepoGroup(kotlin, gfi, "octo/repo", group(issue(1)))

        assertNull(repository.load(kotlin, gfi))
        assertEquals(0, storage.writes)
    }

    @Test
    fun clearAllRemovesEveryCombination() = runBlocking {
        val storage = FakeStorage()
        val repository = repository(storage)
        repository.save(kotlin, gfi, emptyList(), 0, 1, true)
        repository.save(kotlin, help, emptyList(), 0, 1, true)

        repository.clearAll()

        assertNull(repository.load(kotlin, gfi))
        assertNull(repository.load(kotlin, help))
        assertEquals(1, storage.clears)
    }

    /** A new repository over the same storage stands in for killing and relaunching the app. */
    @Test
    fun savedEntriesSurviveARestart() = runBlocking {
        val storage = FakeStorage()
        repository(storage).save(kotlin, gfi, listOf(group(issue(1))), 5, 2, true)

        val afterRestart = repository(storage)

        assertEquals(5, afterRestart.load(kotlin, gfi)!!.totalIssueCount)
    }

    @Test
    fun theCacheKeyCombinesLanguageAndMode() {
        assertEquals("KOTLIN|GOOD_FIRST_ISSUE", cacheKeyFor(kotlin, gfi))
        assertTrue(cacheKeyFor(kotlin, gfi) != cacheKeyFor(kotlin, help))
    }
}
