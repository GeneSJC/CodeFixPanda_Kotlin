package com.example.githubissuefinder.ui.discover

import com.example.githubissuefinder.data.RepoIssuesState
import com.example.githubissuefinder.data.SearchFailure
import com.example.githubissuefinder.testutil.repoGroupOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The wording the repo-issues screen shows for the state holder's facts, and the state-to-UI mapping. */
class RepoIssuesUiMappingTest {

    @Test
    fun anHttpFailureSaysTheStatusCode() {
        assertEquals("GitHub returned an error (HTTP 500)", SearchFailure.Http(500).toRefreshMessage())
    }

    @Test
    fun aFailureBeforeGitHubAnsweredSaysWhatWentWrong() {
        assertEquals("Couldn't refresh this repo: no network", SearchFailure.Failed("no network").toRefreshMessage())
        assertEquals("Couldn't refresh this repo: unknown error", SearchFailure.Failed(null).toRefreshMessage())
    }

    @Test
    fun theStateMapsFieldForFieldWithTheMessageAdded() {
        val issues = repoGroupOf("octo", "repo", 1, 2).issues
        val state = RepoIssuesState(
            issues = issues,
            isRefreshing = true,
            hasRefreshed = true,
            lastRefreshedAtMs = 777L,
            refreshFailure = SearchFailure.Http(500),
        )

        val ui = state.toUiState()

        assertEquals(issues, ui.issues)
        assertTrue(ui.isRefreshing)
        assertTrue(ui.hasRefreshed)
        assertEquals(777L, ui.lastRefreshedAtMs)
        assertEquals("GitHub returned an error (HTTP 500)", ui.refreshError)
    }

    @Test
    fun noFailureMapsToNoMessage() {
        val ui = RepoIssuesState().toUiState()

        assertNull(ui.refreshError)
        assertFalse(ui.hasRefreshed)
    }
}
