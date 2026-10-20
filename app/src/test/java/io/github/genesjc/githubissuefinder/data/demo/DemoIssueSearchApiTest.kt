package io.github.genesjc.githubissuefinder.data.demo

import io.github.genesjc.githubissuefinder.data.IssueSearchRepository
import io.github.genesjc.githubissuefinder.data.IssueSearchResult
import io.github.genesjc.githubissuefinder.data.ReadmeRepository
import io.github.genesjc.githubissuefinder.data.ReadmeResult
import io.github.genesjc.githubissuefinder.data.repoIssuesQuery
import io.github.genesjc.githubissuefinder.data.topReposQuery
import io.github.genesjc.githubissuefinder.model.DEMO_TAG
import io.github.genesjc.githubissuefinder.model.IssueLabelFilter
import io.github.genesjc.githubissuefinder.model.LanguageFilter
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

        assertEquals(listOf("demo-data-tools"), items.map { it.repositoryUrl.substringAfterLast('/') }.distinct())
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
    }
}
