package io.github.genesjc.opensourcepanda.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadmeSnippetRepositoryTest {

    /** A fake source that replays a queue of results and counts how often it is asked. */
    private class FakeSource(vararg results: ReadmeFetch) : ReadmeSource {
        private val queue = results.toMutableList()
        var calls = 0
        override suspend fun fetch(repoOwner: String, repoName: String): ReadmeFetch {
            calls++
            return if (queue.size > 1) queue.removeAt(0) else queue.first()
        }
    }

    private fun load(repository: ReadmeSnippetRepository, name: String = "octo/repo") = runBlocking {
        repository.loadIfNeeded(name.substringBefore('/'), name.substringAfter('/'), name)
    }

    @Test
    fun startsWithNothingLoaded() {
        assertTrue(ReadmeSnippetRepository(FakeSource(ReadmeFetch.Transient)).snippets.value.isEmpty())
    }

    @Test
    fun aResolvedReadmeBecomesASnippet() {
        val repository = ReadmeSnippetRepository(FakeSource(ReadmeFetch.Resolved("# Title\n\nSome text.")))
        load(repository)
        assertEquals("Title Some text.", repository.snippets.value["octo/repo"])
    }

    @Test
    fun aResolvedSnippetIsFetchedOnlyOnce() {
        val source = FakeSource(ReadmeFetch.Resolved("Hello"))
        val repository = ReadmeSnippetRepository(source)

        load(repository)
        load(repository)

        assertEquals(1, source.calls)
    }

    @Test
    fun aRepoWithNoReadmeIsRecordedAsNoPreviewAndNotRetried() {
        val source = FakeSource(ReadmeFetch.Resolved(null))
        val repository = ReadmeSnippetRepository(source)

        load(repository)
        load(repository)

        assertTrue(repository.snippets.value.containsKey("octo/repo"))
        assertNull(repository.snippets.value["octo/repo"])
        assertEquals(1, source.calls)
    }

    /** The real bug this guards against: one transient failure used to leave a card's preview blank for the whole session. */
    @Test
    fun aTransientFailureIsRetriedNextTime() {
        val source = FakeSource(ReadmeFetch.Transient, ReadmeFetch.Resolved("Recovered"))
        val repository = ReadmeSnippetRepository(source)

        load(repository)
        assertFalse(repository.snippets.value.containsKey("octo/repo"))

        load(repository)
        assertEquals("Recovered", repository.snippets.value["octo/repo"])
        assertEquals(2, source.calls)
    }

    @Test
    fun differentReposAreTrackedSeparately() {
        val repository = ReadmeSnippetRepository(FakeSource(ReadmeFetch.Resolved("Text")))

        load(repository, "a/one")
        load(repository, "b/two")

        assertEquals(setOf("a/one", "b/two"), repository.snippets.value.keys)
    }
}
