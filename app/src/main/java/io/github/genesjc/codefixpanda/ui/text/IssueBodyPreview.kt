package io.github.genesjc.codefixpanda.ui.text

import io.github.genesjc.codefixpanda.domain.markdownToPlainText

/**
 * A plain-text preview of an issue's Markdown body for the card, which cuts it at three lines. The
 * cleanup is the one the README snippets use (markdownToPlainText); this is the name the cards call it
 * by, since a composable reads no domain code directly.
 */
fun issueBodyPreview(markdown: String): String = markdownToPlainText(markdown)
