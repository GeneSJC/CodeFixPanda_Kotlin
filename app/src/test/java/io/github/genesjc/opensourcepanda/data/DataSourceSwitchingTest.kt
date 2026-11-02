package io.github.genesjc.opensourcepanda.data

import io.github.genesjc.opensourcepanda.data.demo.InMemoryRepoIssueGroupStorage
import io.github.genesjc.opensourcepanda.model.RateLimitInfo
import io.github.genesjc.opensourcepanda.network.dto.IssueSearchResponseDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The switching data sources send each call to the demo or the real source according to the flag, read on every call. */
class DataSourceSwitchingTest {

    private class NamedApi(private val name: String) : IssueSearchApi {
        override suspend fun searchIssues(query: String, perPage: Int, page: Int) = IssueSearchApiResponse(
            code = 200, isSuccessful = true, body = IssueSearchResponseDto(totalCount = 0), rateLimit = null, tag = name,
        )

        override suspend fun fetchRateLimit(): RateLimitInfo? = null
    }

    private class MapStorage : RepoIssueGroupStorage {
        var entries: Map<String, RepoIssueGroupCacheEntry> = emptyMap()
        var cleared = false
        override fun readAll() = entries
        override fun writeAll(all: Map<String, RepoIssueGroupCacheEntry>) {
            entries = all
        }
        override fun clear() {
            cleared = true
        }
    }

    private val entry = RepoIssueGroupCacheEntry(emptyList(), 0, 1, false, 1L)

    @Test
    fun searchGoesToTheSourceTheFlagPicksOnEveryCall() = runBlocking {
        var demo = true
        val api = SwitchedIssueSearchApi(NamedApi("live"), NamedApi("demo"), { demo })

        assertEquals("demo", api.searchIssues("q", 30, 1).tag)
        demo = false
        assertEquals("live", api.searchIssues("q", 30, 1).tag)
    }

    @Test
    fun readmeGoesToTheSourceTheFlagPicks() = runBlocking {
        var demo = false
        val live = ReadmeApi { _, _ -> ReadmeApiResponse(200, true, null) }
        val demoApi = ReadmeApi { _, _ -> ReadmeApiResponse(404, false, null) }
        val api = SwitchedReadmeApi(live, demoApi, { demo })

        assertEquals(200, api.getReadme("o", "r").code)
        demo = true
        assertEquals(404, api.getReadme("o", "r").code)
    }

    @Test
    fun theCacheIsKeptSeparatelyForDemoAndReal() {
        var demo = true
        val live = MapStorage()
        val demoStorage = InMemoryRepoIssueGroupStorage()
        val storage = SwitchedRepoIssueGroupStorage(live, demoStorage, { demo })

        storage.writeAll(mapOf("k" to entry))
        assertTrue(live.entries.isEmpty())
        assertEquals(mapOf("k" to entry), storage.readAll())

        demo = false
        assertTrue(storage.readAll().isEmpty())
        storage.clear()
        assertTrue(live.cleared)
        assertEquals(mapOf("k" to entry), demoStorage.readAll())
    }

    @Test
    fun theInMemoryStorageClearsToEmpty() {
        val storage = InMemoryRepoIssueGroupStorage()
        storage.writeAll(mapOf("k" to entry))

        storage.clear()

        assertTrue(storage.readAll().isEmpty())
    }
}
