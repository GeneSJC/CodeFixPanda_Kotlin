package com.example.githubissuefinder.data

import androidx.compose.runtime.mutableStateMapOf
import com.example.githubissuefinder.network.GitHubApiClient
import com.example.githubissuefinder.network.decodeGitHubContentsBase64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val SNIPPET_MAX_CHARS = 600

/**
 * A short, plain-text preview of each repo's README, shown on its
 * RepoCard (TopReposScreen) beneath the title row, collapsed to 2
 * lines with a tap-to-expand toggle revealing the rest of the stored
 * snippet (up to SNIPPET_MAX_CHARS) — no further network call on
 * expand, just a maxLines change against text already fetched.
 *
 * Plain in-memory Kotlin singleton (mutableStateMapOf, same reactive-
 * without-a-ViewModel pattern RateLimitState already uses
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
 * guard means a SUCCESSFUL fetch (including a legitimate "no README"
 * 404) only ever happens once per repo per process lifetime — but see
 * the next paragraph for what "successful" deliberately excludes.
 *
 * RETRY ON TRANSIENT FAILURE ONLY: a real, observed bug — two repos
 * (Contextium, Stash) showed their README fine in RepoIssuesScreen's
 * full README widget (a separate fetch, RepoReadmeViewModel, triggered
 * on-demand one at a time) but never got a preview here at all. Root
 * cause: the original version of this store marked a repo "handled"
 * the moment loadIfNeeded was first called, success or failure alike —
 * so a transient failure during the list's initial load burst (many
 * cards' Contents-API calls firing at nearly the same moment) left
 * that card's preview permanently empty for the rest of the session,
 * with no way to recover short of restarting the app. Fixed by only
 * treating a THROWN exception as transient and worth retrying (removed
 * from inFlightOrLoaded, so a later recomposition — e.g. scrolling the
 * card off-screen and back — tries again); an actual HTTP response
 * (including a 404 genuinely meaning "no README") is still treated as
 * resolved and not retried, since that's a deterministic answer from
 * GitHub, not a blip.
 */
object RepoReadmeSnippetStore {
    private val snippets = mutableStateMapOf<String, String?>()
    private val inFlightOrLoaded = mutableSetOf<String>()

    fun snippetFor(repoFullName: String): String? = snippets[repoFullName]

    suspend fun loadIfNeeded(repoOwner: String, repoName: String, repoFullName: String) {
        if (!inFlightOrLoaded.add(repoFullName)) return
        if (!GitHubApiClient.hasToken) {
            inFlightOrLoaded.remove(repoFullName)
            return
        }

        try {
            val response = withContext(Dispatchers.IO) {
                GitHubApiClient.api.getReadme(repoOwner, repoName)
            }
            val body = response.body()
            val snippet = if (!response.isSuccessful || body == null) {
                null
            } else {
                decodeGitHubContentsBase64(body.content, body.encoding)?.let(::buildSnippet)
            }
            snippets[repoFullName] = snippet
        } catch (e: Exception) {
            // Transient (network/timeout) — allow a later retry rather
            // than permanently caching this as "no preview."
            inFlightOrLoaded.remove(repoFullName)
        }
    }

    /**
     * Light markdown-to-plain-text cleanup — strips HTML tags entirely
     * (common at the top of a README: centered logo/badge blocks like
     * `<div align="center"><img src="..."></div>`, which would
     * otherwise show up as literal "<img src=..." junk in a plain Text,
     * since this never goes through Markwon the way the full README
     * view does), strips heading/list/quote markdown markers, and
     * collapses all whitespace (including blank lines between
     * paragraphs) to single spaces. A line that's ENTIRELY a tag (e.g.
     * a standalone `<img ...>` line) disappears completely rather than
     * leaving an empty line behind; a line that MIXES a tag with real
     * text (e.g. `<p align="center"> FoxyVPN - a VPN client`) keeps the
     * real text and drops just the tag. Not a general markdown/HTML
     * stripper — just enough that a typical README's opening content
     * reads as plain prose instead of showing raw markup.
     *
     * Truncates to SNIPPET_MAX_CHARS at the nearest earlier word
     * boundary (so a word is never cut mid-way) and appends "…" when
     * truncated — this is the full stored snippet; the UI itself
     * separately clamps to 2 lines when collapsed.
     */
    private fun buildSnippet(markdown: String): String? {
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
}
