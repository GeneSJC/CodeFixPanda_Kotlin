package io.github.genesjc.codefixpanda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** buildReadmeSnippet: the markdown-to-plain-text cleanup behind each repo card's README preview. */
class ReadmeSnippetTest {

    @Test
    fun plainProseIsKeptAsIs() {
        assertEquals("A small library for parsing.", buildReadmeSnippet("A small library for parsing."))
    }

    @Test
    fun headingMarkersAreStripped() {
        assertEquals("My Project A description.", buildReadmeSnippet("# My Project\n\nA description."))
    }

    @Test
    fun listAndQuoteMarkersAreStripped() {
        assertEquals("one two three", buildReadmeSnippet("- one\n* two\n> three"))
    }

    @Test
    fun blankLinesAndExtraWhitespaceCollapseToSingleSpaces() {
        assertEquals("first second", buildReadmeSnippet("first\n\n\n   second   "))
    }

    @Test
    fun aLineThatIsOnlyAnHtmlTagDisappearsCompletely() {
        assertEquals("Real text", buildReadmeSnippet("<img src=\"logo.png\">\nReal text"))
    }

    @Test
    fun aLineMixingATagWithTextKeepsTheText() {
        assertEquals("FoxyVPN - a VPN client", buildReadmeSnippet("<p align=\"center\"> FoxyVPN - a VPN client"))
    }

    @Test
    fun aReadmeThatIsOnlyMarkupHasNoSnippet() {
        assertNull(buildReadmeSnippet("<div align=\"center\"><img src=\"x.png\"></div>"))
    }

    @Test
    fun anEmptyReadmeHasNoSnippet() {
        assertNull(buildReadmeSnippet("   \n\n  "))
    }

    @Test
    fun aLongReadmeIsCutAtAWordBoundaryWithAnEllipsis() {
        val text = (1..400).joinToString(" ") { "word$it" }

        val snippet = buildReadmeSnippet(text)!!

        assertTrue(snippet.endsWith("…"))
        assertTrue(snippet.length <= 601)
        // Cut on a space: the text before the ellipsis never ends mid-word.
        assertTrue(text.startsWith(snippet.removeSuffix("…")))
        assertTrue(text[snippet.removeSuffix("…").length] == ' ')
    }

    @Test
    fun aReadmeExactlyAtTheLimitIsNotTruncated() {
        val text = "a".repeat(600)
        assertEquals(text, buildReadmeSnippet(text))
    }
}
