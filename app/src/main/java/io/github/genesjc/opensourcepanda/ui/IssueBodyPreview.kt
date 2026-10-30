package io.github.genesjc.opensourcepanda.ui

private val HTML_COMMENT = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)
private val HTML_TAG = Regex("<[^>]+>")
private val MARKDOWN_IMAGE = Regex("!\\[[^\\]]*]\\([^)]*\\)")
private val MARKDOWN_LINK = Regex("\\[([^\\]]*)]\\([^)]*\\)")
private val LINE_MARKER = Regex("(?m)^\\s{0,3}(#{1,6}|>|[-*+]|\\d+\\.)\\s+")
private val EMPHASIS = Regex("\\*\\*|__|~~|`")
private val WHITESPACE = Regex("\\s+")

/**
 * A plain-text preview of an issue's Markdown body for the card: issue templates' hidden comments, HTML tags and
 * images are dropped, a link keeps its text, the heading, quote and list markers and the bold and code marks go,
 * and the rest is one run of text for the card to cut at three lines. Pure, so it is directly tested.
 */
fun issueBodyPreview(markdown: String): String = markdown
    .replace(HTML_COMMENT, " ")
    .replace(HTML_TAG, " ")
    .replace(MARKDOWN_IMAGE, " ")
    .replace(MARKDOWN_LINK, "$1")
    .replace(LINE_MARKER, "")
    .replace(EMPHASIS, "")
    .replace(WHITESPACE, " ")
    .trim()
