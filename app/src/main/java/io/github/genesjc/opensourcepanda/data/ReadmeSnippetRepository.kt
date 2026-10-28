package io.github.genesjc.opensourcepanda.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val SNIPPET_MAX_CHARS = 600

/** The outcome of asking where READMEs come from for one repo. */
sealed interface ReadmeFetch {
    /**
     * GitHub gave a definite answer: [markdown] is the README's text, or
     * null if the repo has none (a real 404) or it couldn't be decoded.
     * Either way it is final and not retried.
     */
    data class Resolved(val markdown: String?) : ReadmeFetch

    /** A network failure or timeout: worth trying again later, not cached as "no preview". */
    data object Transient : ReadmeFetch
}

/**
 * Where a repo's README text comes from, split out so the cache's own
 * rules (load once, retry only transient failures) can be unit-tested
 * against a fake with no network.
 */
fun interface ReadmeSource {
    suspend fun fetch(repoOwner: String, repoName: String): ReadmeFetch
}

/**
 * The real source: asks ReadmeRepository for the README and turns its
 * outcome into what the snippet cache needs (see [toReadmeFetch]).
 */
class GitHubReadmeSource(
    private val repository: ReadmeRepository,
) : ReadmeSource {
    override suspend fun fetch(repoOwner: String, repoName: String): ReadmeFetch =
        repository.fetch(repoOwner, repoName).toReadmeFetch()
}

/**
 * How a README outcome counts for the preview cache. A real answer from
 * GitHub — including "no README" (404), an error response, or a README
 * that can't be decoded — is final: there is simply no preview, and it is
 * not retried. A failed request is transient and worth retrying.
 */
fun ReadmeResult.toReadmeFetch(): ReadmeFetch = when (this) {
    is ReadmeResult.Found -> ReadmeFetch.Resolved(markdown)
    ReadmeResult.NotFound, is ReadmeResult.HttpError, ReadmeResult.Unreadable -> ReadmeFetch.Resolved(null)
    is ReadmeResult.Failed -> ReadmeFetch.Transient
}

/**
 * A short plain-text preview of each repo's README, for its card. Kept in memory as a StateFlow,
 * not on disk: a snippet is cheap to refetch and need not survive process death. The ViewModel
 * asks it to load a snippet when the card first appears.
 *
 * Each card's first appearance costs one Contents API call (a different, far larger quota than
 * search), so the dozens of cards after Load More add up. A real answer from GitHub, including a
 * 404 "no README", is fetched once per repo per process; only a thrown exception is treated as
 * transient and retried on a later recomposition. More in docs/design-notes.md, "README previews".
 */
class ReadmeSnippetRepository(private val source: ReadmeSource) {

    private val inFlightOrLoaded = mutableSetOf<String>()

    private val _snippets = MutableStateFlow<Map<String, String?>>(emptyMap())

    /** Snippets by repo full name. A key with a null value means "resolved: no preview"; a missing key means not loaded yet. */
    val snippets: StateFlow<Map<String, String?>> = _snippets.asStateFlow()

    suspend fun loadIfNeeded(repoOwner: String, repoName: String, repoFullName: String) {
        if (!inFlightOrLoaded.add(repoFullName)) return
        var resolved = false
        try {
            val result = source.fetch(repoOwner, repoName)
            if (result is ReadmeFetch.Resolved) {
                _snippets.value = _snippets.value + (repoFullName to result.markdown?.let(::buildReadmeSnippet))
                resolved = true
            }
        } finally {
            // Anything but a definite answer — a transient failure, or this
            // call being cancelled (a card scrolled away mid-load) — leaves
            // the repo eligible for a later retry.
            if (!resolved) inFlightOrLoaded.remove(repoFullName)
        }
    }
}

/**
 * Light markdown-to-plain-text cleanup: strips HTML tags (a README often opens with a centered
 * logo block), heading, list and quote markers, and collapses whitespace. A line that is only a
 * tag disappears; a line mixing a tag with text keeps the text. Truncates to SNIPPET_MAX_CHARS
 * at a word boundary and appends "…". Not a general markdown stripper.
 */
fun buildReadmeSnippet(markdown: String): String? {
    val htmlTag = Regex("<[^>]*>")
    val plain = markdown
        .lineSequence()
        .map { it.replace(htmlTag, " ").trim().trimStart('#', '-', '*', '>').trim() }
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .trim()
    if (plain.isBlank()) return null
    if (plain.length <= SNIPPET_MAX_CHARS) return plain
    val cut = plain.substring(0, SNIPPET_MAX_CHARS)
    val lastSpace = cut.lastIndexOf(' ')
    val trimmedCut = if (lastSpace > SNIPPET_MAX_CHARS / 2) cut.substring(0, lastSpace) else cut
    return "$trimmedCut…"
}
