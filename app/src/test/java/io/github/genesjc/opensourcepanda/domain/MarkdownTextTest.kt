package io.github.genesjc.opensourcepanda.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** markdownToPlainText and cutAtWord: the cleanup behind both the README snippets and the issue body previews. */
class MarkdownTextTest {

    @Test
    fun plainTextPassesThroughWithOneSpaceBetweenWords() {
        assertEquals("Fix the crash when the list is empty.", markdownToPlainText("Fix the crash  when the\nlist is empty.\n"))
    }

    @Test
    fun hiddenCommentsAndTagsAreDropped() {
        assertEquals("Steps to reproduce", markdownToPlainText("<!-- Describe the bug -->\n<b>Steps</b> to reproduce"))
    }

    @Test
    fun aLinkKeepsItsTextAndAnImageGoes() {
        assertEquals("See the design doc for details", markdownToPlainText("See the [design doc](https://example.com/d) ![shot](https://example.com/s.png) for details"))
    }

    @Test
    fun headingQuoteAndListMarkersAndEmphasisGo() {
        assertEquals("Title quoted first second bold code", markdownToPlainText("# Title\n> quoted\n- first\n1. second\n**bold** `code`"))
    }

    @Test
    fun underscoresInsideWordsStay() {
        assertEquals("rename snake_case_name", markdownToPlainText("rename snake_case_name"))
    }

    @Test
    fun anEmptyBodyIsEmpty() {
        assertEquals("", markdownToPlainText("  \n "))
    }

    @Test
    fun horizontalRulesGo() {
        assertEquals("Title Body", markdownToPlainText("Title\n\n---\n\nBody\n***\n"))
    }

    @Test
    fun textThatFitsIsNotCut() {
        assertEquals("short", "short".cutAtWord(10))
        assertEquals("a".repeat(10), "a".repeat(10).cutAtWord(10))
    }

    @Test
    fun longTextIsCutAtAWordWithAnEllipsis() {
        assertEquals("one two…", "one two three four".cutAtWord(10))
    }

    @Test
    fun aCutWithNoLateSpaceFallsBackToMidWord() {
        assertEquals("abcdefghij…", "abcdefghijklmnop".cutAtWord(10))
    }
}
