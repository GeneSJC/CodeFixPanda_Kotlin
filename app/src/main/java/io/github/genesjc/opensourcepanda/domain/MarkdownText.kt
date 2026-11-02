package io.github.genesjc.opensourcepanda.domain

private val HTML_COMMENT = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)
private val HTML_TAG = Regex("<[^>]+>")
private val MARKDOWN_IMAGE = Regex("!\\[[^\\]]*]\\([^)]*\\)")
private val MARKDOWN_LINK = Regex("\\[([^\\]]*)]\\([^)]*\\)")
private val HORIZONTAL_RULE = Regex("(?m)^\\s{0,3}([-*_])(?:\\s*\\1){2,}\\s*$")
private val LINE_MARKER = Regex("(?m)^\\s{0,3}(#{1,6}|>|[-*+]|\\d+\\.)\\s+")
private val EMPHASIS = Regex("\\*\\*|__|~~|`")
private val WHITESPACE = Regex("\\s+")

/**
 * Markdown (with the HTML a README or an issue template mixes in) as one run of plain text: comments,
 * tags and images are dropped, a link keeps its text, horizontal rules, heading, quote and list markers
 * and the bold and code marks go, and all whitespace collapses to single spaces. The one cleanup behind
 * both the README snippet on a repo card and the body preview on an issue card. Not a general Markdown
 * renderer. Pure, so it is directly tested.
 */
fun markdownToPlainText(markdown: String): String = markdown
    .replace(HTML_COMMENT, " ")
    .replace(HTML_TAG, " ")
    .replace(MARKDOWN_IMAGE, " ")
    .replace(MARKDOWN_LINK, "$1")
    .replace(HORIZONTAL_RULE, "")
    .replace(LINE_MARKER, "")
    .replace(EMPHASIS, "")
    .replace(WHITESPACE, " ")
    .trim()

/**
 * This text cut to at most [maxChars] characters, at a word boundary when one falls in the second half
 * of the cut (otherwise mid-word), with "…" appended. Text that already fits is returned as it is.
 */
fun String.cutAtWord(maxChars: Int): String {
    if (length <= maxChars) return this
    val cut = substring(0, maxChars)
    val lastSpace = cut.lastIndexOf(' ')
    val trimmedCut = if (lastSpace > maxChars / 2) cut.substring(0, lastSpace) else cut
    return "$trimmedCut…"
}
