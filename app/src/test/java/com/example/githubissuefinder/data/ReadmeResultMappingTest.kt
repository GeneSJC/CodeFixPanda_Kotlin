package com.example.githubissuefinder.data

import com.example.githubissuefinder.R
import com.example.githubissuefinder.testutil.FakeStringResolver
import com.example.githubissuefinder.ui.discover.toUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** How a README outcome counts for the repo-card preview cache, and what the README screen shows for it. */
class ReadmeResultMappingTest {

    private val s = FakeStringResolver

    // ---- preview cache ----

    @Test
    fun aFoundReadmeIsResolvedWithItsMarkdown() {
        assertEquals(ReadmeFetch.Resolved("# Hi"), ReadmeResult.Found("# Hi").toReadmeFetch())
    }

    @Test
    fun noReadmeAnErrorResponseAndAnUnreadableBodyAreAllFinalWithNoPreview() {
        assertEquals(ReadmeFetch.Resolved(null), ReadmeResult.NotFound.toReadmeFetch())
        assertEquals(ReadmeFetch.Resolved(null), ReadmeResult.HttpError(500).toReadmeFetch())
        assertEquals(ReadmeFetch.Resolved(null), ReadmeResult.Unreadable.toReadmeFetch())
    }

    @Test
    fun aFailedRequestIsTransientSoItIsRetried() {
        assertEquals(ReadmeFetch.Transient, ReadmeResult.Failed("x").toReadmeFetch())
    }

    // ---- README screen ----

    @Test
    fun foundShowsTheMarkdown() {
        val state = ReadmeResult.Found("# Hi").toUiState(s)
        assertFalse(state.isLoading)
        assertEquals("# Hi", state.markdown)
        assertNull(state.errorMessage)
    }

    @Test
    fun notFoundShowsTheNoReadmeState() {
        val state = ReadmeResult.NotFound.toUiState(s)
        assertFalse(state.isLoading)
        assertTrue(state.notFound)
        assertNull(state.errorMessage)
    }

    @Test
    fun theErrorStatesKeepTheirExistingWording() {
        assertEquals(s(R.string.error_http, 503), ReadmeResult.HttpError(503).toUiState(s).errorMessage)
        assertEquals(s(R.string.readme_unreadable), ReadmeResult.Unreadable.toUiState(s).errorMessage)
        assertEquals(s(R.string.readme_load_failed, "boom"), ReadmeResult.Failed("boom").toUiState(s).errorMessage)
        assertEquals(s(R.string.readme_load_failed, s(R.string.error_unknown)), ReadmeResult.Failed(null).toUiState(s).errorMessage)
    }

    @Test
    fun everyErrorStateIsNotLoading() {
        listOf(
            ReadmeResult.HttpError(500), ReadmeResult.Unreadable, ReadmeResult.Failed("x"),
        ).forEach { assertFalse(it.toUiState(s).isLoading) }
    }
}
