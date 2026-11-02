package io.github.genesjc.opensourcepanda.ui.text

import org.junit.Assert.assertEquals
import org.junit.Test

/** The plain-text preview of an issue's Markdown body. */
class IssueBodyPreviewTest {

    @Test
    fun plainTextPassesThroughWithOneSpaceBetweenWords() {
        assertEquals("Fix the crash when the list is empty.", issueBodyPreview("Fix the crash  when the\nlist is empty.\n"))
    }

    @Test
    fun hiddenCommentsAndTagsAreDropped() {
        assertEquals("Steps to reproduce", issueBodyPreview("<!-- Describe the bug -->\n<b>Steps</b> to reproduce"))
    }

    @Test
    fun aLinkKeepsItsTextAndAnImageGoes() {
        assertEquals("See the design doc for details", issueBodyPreview("See the [design doc](https://example.com/d) ![shot](https://example.com/s.png) for details"))
    }

    @Test
    fun headingQuoteAndListMarkersAndEmphasisGo() {
        assertEquals("Title quoted first second bold code", issueBodyPreview("# Title\n> quoted\n- first\n1. second\n**bold** `code`"))
    }

    @Test
    fun underscoresInsideWordsStay() {
        assertEquals("rename snake_case_name", issueBodyPreview("rename snake_case_name"))
    }

    @Test
    fun anEmptyBodyIsEmpty() {
        assertEquals("", issueBodyPreview("  \n "))
    }
}
