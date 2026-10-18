package com.example.githubissuefinder.ui.discover

import com.example.githubissuefinder.data.TopReposEvent
import com.example.githubissuefinder.data.SearchFailure
import com.example.githubissuefinder.data.TopReposState
import com.example.githubissuefinder.model.LanguageFilter
import com.example.githubissuefinder.testutil.repoGroupOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The wording the Top Repos screen shows for the state holder's facts, and the state-to-UI mapping. */
class TopReposUiMappingTest {

    @Test
    fun anHttpFailureSaysTheStatusCode() {
        assertEquals("GitHub returned an error (HTTP 500)", SearchFailure.Http(500).toMessage())
    }

    @Test
    fun aFailureBeforeGitHubAnsweredSaysWhatWentWrong() {
        assertEquals("Couldn't load issues: boom", SearchFailure.Failed("boom").toMessage())
        assertEquals("Couldn't load issues: unknown error", SearchFailure.Failed(null).toMessage())
    }

    @Test
    fun theRateLimitEventHasItsOwnWording() {
        assertEquals("GitHub's search rate limit was reached — try again shortly", TopReposEvent.RateLimited.toMessage())
    }

    @Test
    fun newReposAreCountedWithTheRightPlurals() {
        assertEquals("1 new repo, 1 new issue", TopReposEvent.NewReposChecked(1, 1, null, 0L).toMessage())
        assertEquals("2 new repos, 3 new issues", TopReposEvent.NewReposChecked(2, 3, null, 0L).toMessage())
    }

    @Test
    fun nothingNewSaysSince() {
        val message = TopReposEvent.NewReposChecked(0, 0, previousUpdatedAtMs = null, checkedAtMs = System.currentTimeMillis()).toMessage()
        assertTrue(message, message.startsWith("Nothing new since"))
    }

    @Test
    fun checkFailuresHaveTheirOwnWording() {
        assertEquals("Couldn't check for new repos (HTTP 500)", TopReposEvent.CheckFailedHttp(500).toMessage())
        assertEquals("Couldn't check for new repos: boom", TopReposEvent.CheckFailed("boom").toMessage())
        assertEquals("Couldn't check for new repos: unknown error", TopReposEvent.CheckFailed(null).toMessage())
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
            noTokenConfigured = true,
            tokenRejected = true,
            loadFailure = SearchFailure.Http(401),
            loadMoreFailure = SearchFailure.Failed("boom"),
        )

        val ui = state.toUiState()

        assertEquals(listOf(group), ui.repoGroups)
        assertEquals(LanguageFilter.JAVA, ui.selectedLanguage)
        assertEquals(40, ui.totalIssueCount)
        assertEquals(3, ui.currentPage)
        assertFalse(ui.canLoadMore)
        assertEquals(777L, ui.lastUpdatedAtMs)
        assertTrue(ui.isLoading)
        assertTrue(ui.isLoadingMore)
        assertTrue(ui.noTokenConfigured)
        assertTrue(ui.tokenRejected)
        assertEquals("GitHub returned an error (HTTP 401)", ui.errorMessage)
        assertEquals("Couldn't load issues: boom", ui.loadMoreError)
    }

    @Test
    fun noFailureMapsToNoMessage() {
        val ui = TopReposState().toUiState()

        assertNull(ui.errorMessage)
        assertNull(ui.loadMoreError)
    }
}
