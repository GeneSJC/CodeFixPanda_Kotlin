package io.github.genesjc.githubissuefinder.ui.discover

import io.github.genesjc.githubissuefinder.R
import io.github.genesjc.githubissuefinder.data.SearchFailure
import io.github.genesjc.githubissuefinder.data.TopReposEvent
import io.github.genesjc.githubissuefinder.data.TopReposState
import io.github.genesjc.githubissuefinder.model.LanguageFilter
import io.github.genesjc.githubissuefinder.testutil.FakeStringResolver
import io.github.genesjc.githubissuefinder.testutil.repoGroupOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The wording the Top Repos screen shows for the state holder's facts, and the state-to-UI mapping. */
class TopReposUiMappingTest {

    private val s = FakeStringResolver

    @Test
    fun anHttpFailureSaysTheStatusCode() {
        assertEquals(s(R.string.error_http, 500), SearchFailure.Http(500).toMessage(s))
    }

    @Test
    fun aFailureBeforeGitHubAnsweredSaysWhatWentWrong() {
        assertEquals(s(R.string.load_issues_failed, "boom"), SearchFailure.Failed("boom").toMessage(s))
        assertEquals(s(R.string.load_issues_failed, s(R.string.error_unknown)), SearchFailure.Failed(null).toMessage(s))
    }

    @Test
    fun theRateLimitEventHasItsOwnWording() {
        assertEquals(s(R.string.search_rate_limited), TopReposEvent.RateLimited.toMessage(s))
    }

    @Test
    fun newReposAreCountedWithTheRightPlurals() {
        assertEquals(
            s(R.string.new_repos_and_issues, s.plural(R.plurals.new_repos, 1, 1), s.plural(R.plurals.new_issues, 1, 1)),
            TopReposEvent.NewReposChecked(1, 1, null, 0L).toMessage(s),
        )
        assertEquals(
            s(R.string.new_repos_and_issues, s.plural(R.plurals.new_repos, 2, 2), s.plural(R.plurals.new_issues, 3, 3)),
            TopReposEvent.NewReposChecked(2, 3, null, 0L).toMessage(s),
        )
    }

    @Test
    fun nothingNewSaysSince() {
        val message = TopReposEvent.NewReposChecked(0, 0, previousUpdatedAtMs = null, checkedAtMs = System.currentTimeMillis()).toMessage(s)
        assertTrue(message, message.startsWith("string:${R.string.nothing_new_since}("))
    }

    @Test
    fun checkFailuresHaveTheirOwnWording() {
        assertEquals(s(R.string.check_new_repos_failed_http, 500), TopReposEvent.CheckFailedHttp(500).toMessage(s))
        assertEquals(s(R.string.check_new_repos_failed, "boom"), TopReposEvent.CheckFailed("boom").toMessage(s))
        assertEquals(
            s(R.string.check_new_repos_failed, s(R.string.error_unknown)),
            TopReposEvent.CheckFailed(null).toMessage(s),
        )
    }

    @Test
    fun theStateMapsFieldForFieldWithMessagesAdded() {
        val group = repoGroupOf("a", "one", 1)
        val state = TopReposState(
            repoGroups = listOf(group),
            selectedLanguage = LanguageFilter.JAVA,
            totalIssueCount = 40,
            currentPage = 3,
            canLoadMore = false,
            lastUpdatedAtMs = 777L,
            isLoading = true,
            isLoadingMore = true,
            tokenRejected = true,
            loadFailure = SearchFailure.Http(401),
            loadMoreFailure = SearchFailure.Failed("boom"),
        )

        val ui = state.toUiState(s)

        assertEquals(listOf(group), ui.repoGroups)
        assertEquals(LanguageFilter.JAVA, ui.selectedLanguage)
        assertEquals(40, ui.totalIssueCount)
        assertEquals(3, ui.currentPage)
        assertFalse(ui.canLoadMore)
        assertEquals(777L, ui.lastUpdatedAtMs)
        assertTrue(ui.isLoading)
        assertTrue(ui.isLoadingMore)
        assertTrue(ui.tokenRejected)
        assertEquals(s(R.string.error_http, 401), ui.errorMessage)
        assertEquals(s(R.string.load_issues_failed, "boom"), ui.loadMoreError)
    }

    @Test
    fun noFailureMapsToNoMessage() {
        val ui = TopReposState().toUiState(s)

        assertNull(ui.errorMessage)
        assertNull(ui.loadMoreError)
    }
}
