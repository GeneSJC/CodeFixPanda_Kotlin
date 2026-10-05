package com.example.githubissuefinder.data

import androidx.compose.runtime.mutableStateMapOf
import com.example.githubissuefinder.network.GitHubApiClient
import com.example.githubissuefinder.network.decodeGitHubContentsBase64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val SNIPPET_MAX_CHARS = 150

/**
 * A short, plain-text preview of each repo's README, shown on its
 * RepoCard (TopReposScreen) beneath the title row — a static preview
 * only, per direct instruction. An expandable widget (matching
 * RepoIssuesScreen's own README widget) is a possible later step, not
 * this one.
 *
 * Plain in-memory Kotlin singleton (mutableStateMapOf, same reactive-
 * without-a-ViewModel pattern RateLimitState/RepoBookmarks already use
 * elsewhere in this app), NOT persisted to disk — unlike those two, a
 * README snippet is cheap to refetch and has no reason to survive a
 * process death, so there's no need for a second on-disk cache format.
 *
 * COST, worth being direct about: RepoCards on screen can number in
 * the dozens once Load More is used, and each one's first composition
 * triggers ONE Contents API call here (GET /repos/{owner}/{repo}/readme)
 * — a different, far larger quota (5,000/hour authenticated) than the
 * Search API's 30/minute RateLimitState tracks, same distinction
 * RepoReadmeViewModel's own doc comment makes. loadIfNeeded's dedup
 * guard (inFlightOrLoaded) means this only ever happens ONCE per repo
 * per process lifetime, not on every recomposition or scroll back into
 * view — but it's still a real, additional cost layered on top of
 * whatever Search API calls built the list in the first place, worth
 * knowing about rather than treating as free.
 */
object RepoReadmeSnippetStore {
    private val snippets = mutableStateMapOf<String, String?>()
    private val inFlightOrLoaded = mutableSetOf<String>()

    fun snippetFor(repoFullName: String): String? = snippets[repoFullName]

    suspend fun loadIfNeeded(repoOwner: String, repoName: String, repoFullName: String) {
        if (!inFlightOrLoaded.add(repoFullName)) return
        if (!GitHubApiClient.hasToken) return

        val snippet = try {
            val response = withContext(Dispatchers.IO) {
                GitHubApiClient.api.getReadme(repoOwner, repoName)
            }
            val body = response.body()
            if (!response.isSuccessful || body == null) {
                null
            } else {
                decodeGitHubContentsBase64(body.content, body.encoding)?.let(::buildSnippet)
            }
        } catch (e: Exception) {
            null
        }
        snippets[repoFullName] = snippet
    }

    /**
     * Light markdown-to-plain-text cleanup — strips heading/list/quote
     * markers and collapses all whitespace (including blank lines
     * between paragraphs) to single spaces, since this renders as
     * plain Text, not through Markwon the way the full README view
     * does. Not a general markdown stripper, just enough that a
     * typical README's opening lines ("# Project\n\nA short
     * description...") read as a normal sentence instead of showing
     * literal "#" characters and raw line breaks. Truncates to
     * SNIPPET_MAX_CHARS at the nearest earlier word boundary (so a
     * word is never cut mid-way) and appends "…" when truncated.
     */
    private fun buildSnippet(markdown: String): String? {
        val plain = markdown
            .lineSequence()
            .map { it.trim().trimStart('#', '-', '*', '>').trim() }
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
}
