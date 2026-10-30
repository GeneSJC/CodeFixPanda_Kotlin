package io.github.genesjc.opensourcepanda.ui

import io.github.genesjc.opensourcepanda.R
import io.github.genesjc.opensourcepanda.model.Issue
import io.github.genesjc.opensourcepanda.testutil.FakeStringResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The line under an issue's title, over a fake resolver. */
class IssueMetaTest {

    private val s = FakeStringResolver
    private val hour = 60L * 60 * 1000
    private val now = 100L * hour

    private fun issue(number: Int, createdAtMs: Long?, updatedAtMs: Long?, author: String?) = Issue(
        id = 1L, title = "t", bodyText = "", repoOwner = "octo", repoName = "repo", labels = emptyList(),
        language = null, commentCount = 0, updatedAtMs = updatedAtMs, htmlUrl = "https://github.com/octo/repo/issues/$number",
        number = number, createdAtMs = createdAtMs, authorLogin = author,
    )

    @Test
    fun numberOpenedTimeAndAuthor() {
        val opened = s.relativeTime(now - 22 * hour, now)
        assertEquals(s(R.string.issue_meta_opened_by, 89, opened, "sarah_dev"), s.issueMeta(issue(89, now - 22 * hour, now, "sarah_dev"), now))
    }

    @Test
    fun withoutAnAuthorItIsJustNumberAndTime() {
        val opened = s.relativeTime(now - 22 * hour, now)
        assertEquals(s(R.string.issue_meta_opened, 90, opened), s.issueMeta(issue(90, now - 22 * hour, now, null), now))
    }

    @Test
    fun anUnknownOpeningTimeFallsBackToTheLastUpdate() {
        val updated = s.relativeTime(now - 5 * hour, now)
        assertEquals(s(R.string.issue_meta_opened, 91, updated), s.issueMeta(issue(91, null, now - 5 * hour, null), now))
    }

    @Test
    fun anIssueSavedBeforeItsNumberWasRecordedHasNoLine() {
        assertNull(s.issueMeta(issue(0, null, now, null), now))
    }
}
