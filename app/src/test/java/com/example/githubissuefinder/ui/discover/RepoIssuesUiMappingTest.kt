package com.example.githubissuefinder.ui.discover

import com.example.githubissuefinder.R
import com.example.githubissuefinder.data.RepoIssuesState
import com.example.githubissuefinder.data.SearchFailure
import com.example.githubissuefinder.testutil.FakeStringResolver
import com.example.githubissuefinder.testutil.repoGroupOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The wording the repo-issues screen shows for the state holder's facts, and the state-to-UI mapping. */
class RepoIssuesUiMappingTest {

    private val s = FakeStringResolver

    @Test
    fun anHttpFailureSaysTheStatusCode() {
        assertEquals(s(R.string.error_http, 500), SearchFailure.Http(500).toRefreshMessage(s))
    }

    @Test
    fun aFailureBeforeGitHubAnsweredSaysWhatWentWrong() {
        assertEquals(s(R.string.refresh_repo_failed, "no network"), SearchFailure.Failed("no network").toRefreshMessage(s))
        assertEquals(
            s(R.string.refresh_repo_failed, s(R.string.error_unknown)),
            SearchFailure.Failed(null).toRefreshMessage(s),
        )
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

        val ui = state.toUiState(s)

        assertEquals(issues, ui.issues)
        assertTrue(ui.isRefreshing)
        assertTrue(ui.hasRefreshed)
        assertEquals(777L, ui.lastRefreshedAtMs)
        assertEquals(s(R.string.error_http, 500), ui.refreshError)
    }

    @Test
    fun noFailureMapsToNoMessage() {
        val ui = RepoIssuesState().toUiState(s)

        assertNull(ui.refreshError)
        assertFalse(ui.hasRefreshed)
    }
}
