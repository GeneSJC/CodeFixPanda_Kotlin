package com.example.githubissuefinder.data

import com.example.githubissuefinder.ui.discover.toUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** How a README outcome counts for the repo-card preview cache, and what the README screen shows for it. */
class ReadmeResultMappingTest {

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

    @Test
    fun noTokenIsUnavailableSoItIsRetriedOnceThereIsOne() {
        assertEquals(ReadmeFetch.Unavailable, ReadmeResult.NoToken.toReadmeFetch())
    }

    // ---- README screen ----

    @Test
    fun foundShowsTheMarkdown() {
        val state = ReadmeResult.Found("# Hi").toUiState()
        assertFalse(state.isLoading)
        assertEquals("# Hi", state.markdown)
        assertNull(state.errorMessage)
    }

    @Test
    fun notFoundShowsTheNoReadmeState() {
        val state = ReadmeResult.NotFound.toUiState()
        assertFalse(state.isLoading)
        assertTrue(state.notFound)
        assertNull(state.errorMessage)
    }

    @Test
    fun theErrorStatesKeepTheirExistingWording() {
        assertEquals("A GitHub token is required to load a README.", ReadmeResult.NoToken.toUiState().errorMessage)
        assertEquals("GitHub returned an error (HTTP 503)", ReadmeResult.HttpError(503).toUiState().errorMessage)
        assertEquals("Couldn't read this README's content.", ReadmeResult.Unreadable.toUiState().errorMessage)
        assertEquals("Couldn't load the README: boom", ReadmeResult.Failed("boom").toUiState().errorMessage)
        assertEquals("Couldn't load the README: unknown error", ReadmeResult.Failed(null).toUiState().errorMessage)
    }

    @Test
    fun everyErrorStateIsNotLoading() {
        listOf(
            ReadmeResult.NoToken, ReadmeResult.HttpError(500), ReadmeResult.Unreadable, ReadmeResult.Failed("x"),
        ).forEach { assertFalse(it.toUiState().isLoading) }
    }
}
