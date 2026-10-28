package io.github.genesjc.opensourcepanda.data.demo

import io.github.genesjc.opensourcepanda.data.IssueSearchRepository
import io.github.genesjc.opensourcepanda.data.IssueSearchResult
import io.github.genesjc.opensourcepanda.data.ReadmeRepository
import io.github.genesjc.opensourcepanda.data.ReadmeResult
import io.github.genesjc.opensourcepanda.data.repoIssuesQuery
import io.github.genesjc.opensourcepanda.data.topReposQuery
import io.github.genesjc.opensourcepanda.model.DEMO_TAG
import io.github.genesjc.opensourcepanda.model.IssueLabelFilter
import io.github.genesjc.opensourcepanda.model.LanguageFilter
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The demo data source answers the same queries as GitHub, from the fake issues. */
@OptIn(ExperimentalEncodingApi::class)
class DemoIssueSearchApiTest {

    private val api = DemoIssueSearchApi()
    private val gfi = IssueLabelFilter.GOOD_FIRST_ISSUE
    private val helpWanted = IssueLabelFilter.HELP_WANTED

    private fun search(query: String, perPage: Int = 30, page: Int = 1) =
        runBlocking { api.searchIssues(query, perPage, page) }

    @Test
    fun everyDemoIssueIsMarkedDemoAndHasANegativeId() {
        assertTrue(DemoData.issues.all { it.dto.title.startsWith("[Demo] ") })
        assertTrue(DemoData.issues.all { it.dto.id < 0 })
        assertEquals(DemoData.issues.size, DemoData.issues.map { it.dto.id }.toSet().size)
    }

    @Test
    fun aSearchOnlyReturnsIssuesWithTheModesLabel() {
        val items = search(topReposQuery(gfi, LanguageFilter.ANY)).body!!.items

        assertTrue(items.isNotEmpty())
        assertTrue(items.all { issue -> issue.labels.any { it.name == gfi.queryValue } })
    }

    @Test
    fun theLanguageNarrowsTheSearchToThatLanguagesRepos() {
        val items = search(topReposQuery(helpWanted, LanguageFilter.PYTHON)).body!!.items

        val pythonRepos = DemoData.repos.filter { it.language == "python" }.map { it.name }.toSet()
        val found = items.map { it.repositoryUrl.substringAfterLast('/') }.toSet()
        assertTrue(found.isNotEmpty())
        assertTrue(pythonRepos.containsAll(found))
    }

    @Test
    fun everyLanguageHasSeveralReposAndIssuesInBothModes() {
        LanguageFilter.entries.filter { it != LanguageFilter.ANY }.forEach { language ->
            assertTrue(language.name, DemoData.repos.count { it.language == language.queryValue } >= 3)
            listOf(gfi, helpWanted).forEach { mode ->
                val items = search(topReposQuery(mode, language)).body!!.items
                assertTrue("${language.name} / ${mode.name}", items.map { it.repositoryUrl }.distinct().size >= 2)
            }
        }
    }

    @Test
    fun everyIssueBelongsToADeclaredRepoAndNumbersAreUniquePerRepo() {
        val repoNames = DemoData.repos.map { "demo-org/${it.name}" }.toSet()
        assertTrue(DemoData.issues.all { it.repoFullName in repoNames })
        assertEquals(DemoData.issues.size, DemoData.issues.map { it.repoFullName to it.dto.number }.toSet().size)
        assertTrue(DemoData.repos.all { repo -> DemoData.issues.any { it.repoFullName == "demo-org/${repo.name}" } })
    }

    @Test
    fun aRepoQueryReturnsThatReposIssuesOnly() {
        val items = search(repoIssuesQuery("demo-org/demo-api-server", gfi)).body!!.items

        assertTrue(items.isNotEmpty())
        assertTrue(items.all { it.repositoryUrl.endsWith("/demo-api-server") })
    }

    @Test
    fun pagingSlicesTheMatchesAndKeepsTheTotal() {
        val all = search(topReposQuery(gfi, LanguageFilter.ANY))
        val first = search(topReposQuery(gfi, LanguageFilter.ANY), perPage = 2, page = 1)
        val second = search(topReposQuery(gfi, LanguageFilter.ANY), perPage = 2, page = 2)

        assertEquals(all.body!!.totalCount, first.body!!.totalCount)
        assertEquals(all.body!!.items.take(2), first.body!!.items)
        assertEquals(all.body!!.items.drop(2).take(2), second.body!!.items)
    }

    @Test
    fun theResponseCarriesTheDemoTagAndNoRateLimit() {
        val response = search(topReposQuery(gfi, LanguageFilter.ANY))

        assertEquals(DEMO_TAG, response.tag)
        assertNull(response.rateLimit)
        assertNull(runBlocking { api.fetchRateLimit() })
    }

    @Test
    fun theRepositoryTurnsDemoResultsIntoTaggedIssues() = runBlocking {
        val repository = IssueSearchRepository(api)

        val result = repository.searchTopRepos(gfi, LanguageFilter.ANY, page = 1)

        val issues = (result as IssueSearchResult.Success).page.issues
        assertTrue(issues.isNotEmpty())
        assertTrue(issues.all { it.tag == DEMO_TAG })
    }

    @Test
    fun aDemoReadmeIsReadableMarkdown() = runBlocking {
        val repository = ReadmeRepository(
            api = DemoReadmeApi(),
            decode = { content, _ -> content?.let { String(Base64.decode(it), Charsets.UTF_8) } },
        )

        val result = repository.fetch("demo-org", "demo-tasks-app")

        val markdown = (result as ReadmeResult.Found).markdown
        assertTrue(markdown, markdown.startsWith("# demo-tasks-app"))
        assertTrue(markdown, markdown.contains(DemoData.repos.first { it.name == "demo-tasks-app" }.description))
    }
}
