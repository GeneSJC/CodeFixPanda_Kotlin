package com.example.githubissuefinder.data

import android.content.Context
import com.example.githubissuefinder.model.IssueLabelFilter
import com.example.githubissuefinder.model.LanguageFilter
import com.example.githubissuefinder.model.RepoIssueGroup
import com.example.githubissuefinder.network.GitHubApiClient
import com.squareup.moshi.Types
import java.io.File

/**
 * Persists Top Repos' loaded results to a plain JSON file in app-private
 * storage, keyed by (language, Issue Mode) — so switching back to a
 * combination already visited this install is instant and free (zero
 * Search API calls) instead of re-spending calls every time.
 *
 * v0.45, built AFTER pagination (v0.43) rather than before it, on
 * purpose: an earlier draft of this cache was written before Load More
 * existed and was deliberately discarded once pagination landed, per
 * this project's own "stale means rebuild, not patch" rule — that
 * draft only had a single page's worth of data to cache, and pagination
 * changes what "the result for a (language, mode) pair" even means (an
 * accumulated, multi-page set, not one page). CacheEntry below reflects
 * that directly: it stores currentPage and canLoadMore alongside the
 * repo groups, not just the groups themselves, so restoring a cached
 * entry puts Top Repos back exactly where Load More had gotten to, not
 * just back to page 1.
 *
 * A plain JSON file via Context.filesDir, NOT Room — the same "avoid
 * new persistence infrastructure for what's still a small,
 * single-purpose cache" choice this project has made before. Worth
 * revisiting if/when Room is introduced for bookmarks anyway (a real,
 * previously-discussed idea) — one Room database backing both would be
 * a more coherent story than two separate persistence mechanisms, but
 * that's a bigger, separate decision than this CM.
 *
 * ONE combined file (not one file per key) — a single read/write per
 * cache operation is simpler to reason about and cheap enough at this
 * data size (a handful of language/mode combinations) that splitting
 * into per-key files would add file-management complexity for no real
 * benefit yet.
 *
 * Reuses GitHubApiClient.moshi directly rather than building a second
 * Moshi instance — see that file's own comment on why it's exposed now.
 *
 * All I/O here is synchronous (plain File reads/writes) — callers
 * (TopReposViewModel) are responsible for calling this off the main
 * thread (Dispatchers.IO), the same way this project already treats
 * every network call.
 *
 * CACHE-FIRST, NEVER AUTO-REFRESHED: a cache hit is shown as-is, with
 * no automatic background re-fetch to "freshen" it — doing that would
 * quietly spend the exact calls this cache exists to avoid, on every
 * single visit, defeating the point. Getting live data again is always
 * an explicit, visible choice — see TopReposViewModel.checkForNewRepos().
 */
object RepoIssueGroupCache {

    private const val FILE_NAME = "repo_issue_group_cache.json"

    private val mapType = Types.newParameterizedType(
        Map::class.java,
        String::class.java,
        CacheEntry::class.java,
    )
    private val adapter by lazy { GitHubApiClient.moshi.adapter<Map<String, CacheEntry>>(mapType) }

    /**
     * One cached load's worth of data for a single (language, mode)
     * key. repoGroups is the ACCUMULATED, deduped set across however
     * many Load More taps had happened when this was saved — not just
     * one page. currentPage and canLoadMore restore Load More to
     * exactly where it left off, so a returning visit doesn't silently
     * reset pagination progress. fetchedAtMs drives the "Updated X ago"
     * label, so a cached result never silently pretends to be fresher
     * than it is.
     */
    data class CacheEntry(
        val repoGroups: List<RepoIssueGroup>,
        val totalIssueCount: Int,
        val currentPage: Int,
        val canLoadMore: Boolean,
        val fetchedAtMs: Long,
    )

    /** Key is the enums' own .name (e.g. "KOTLIN", "GOOD_FIRST_ISSUE") — stable identifiers, not displayName/queryValue text that could change independently of what they mean. */
    private fun keyFor(language: LanguageFilter, mode: IssueLabelFilter): String =
        "${language.name}|${mode.name}"

    fun load(context: Context, language: LanguageFilter, mode: IssueLabelFilter): CacheEntry? =
        readAll(context)[keyFor(language, mode)]

    fun save(
        context: Context,
        language: LanguageFilter,
        mode: IssueLabelFilter,
        repoGroups: List<RepoIssueGroup>,
        totalIssueCount: Int,
        currentPage: Int,
        canLoadMore: Boolean,
        /**
         * v0.50 ("Refresh this repo" write-back): defaults to now, same
         * as always, for every existing caller (a real list-level fetch
         * DID just happen). replaceRepoGroup() below is the one caller
         * that passes the entry's ORIGINAL fetchedAtMs through instead —
         * refreshing a single repo from its own screen isn't a list-level
         * fetch, so it shouldn't make Top Repos' "Updated X ago" label
         * claim to be fresher than it actually is.
         */
        fetchedAtMs: Long = System.currentTimeMillis(),
    ) {
        val updated = readAll(context).toMutableMap()
        updated[keyFor(language, mode)] = CacheEntry(
            repoGroups = repoGroups,
            totalIssueCount = totalIssueCount,
            currentPage = currentPage,
            canLoadMore = canLoadMore,
            fetchedAtMs = fetchedAtMs,
        )
        writeAll(context, updated)
    }

    /**
     * v0.50 ("Refresh this repo" write-back): updates (or removes) ONE
     * repo's group within an already-cached (language, mode) entry —
     * everything else in that entry (other repos' groups, currentPage,
     * canLoadMore, totalIssueCount, fetchedAtMs) is left exactly as it
     * was. Called after RepoIssuesScreen's own "Refresh this repo"
     * fetch resolves, so the Top Repos card the user navigated from
     * agrees with what the repo screen now shows, without needing a
     * second Top-Repos-level network call.
     *
     * updatedGroup == null means "this repo now has zero open matching
     * issues — remove it," per the plan's "empty repos drop off Top
     * Repos" rule. A no-op if there's no cached entry for this
     * (language, mode) at all — nothing to write back into, e.g. if the
     * repo was somehow reached without Top Repos ever having cached
     * this combination.
     */
    fun replaceRepoGroup(
        context: Context,
        language: LanguageFilter,
        mode: IssueLabelFilter,
        repoFullName: String,
        updatedGroup: RepoIssueGroup?,
    ) {
        val key = keyFor(language, mode)
        val all = readAll(context)
        val entry = all[key] ?: return
        save(
            context = context,
            language = language,
            mode = mode,
            repoGroups = applyRepoGroupReplacement(entry.repoGroups, repoFullName, updatedGroup),
            totalIssueCount = entry.totalIssueCount,
            currentPage = entry.currentPage,
            canLoadMore = entry.canLoadMore,
            fetchedAtMs = entry.fetchedAtMs,
        )
    }

    /**
     * The actual list transform replaceRepoGroup() above needs — split
     * out as its own function purely so it's directly unit-testable
     * without a Context (no file I/O here, unlike the rest of this
     * object): removes any existing group for repoFullName, adds
     * updatedGroup back in if non-null (null means "remove," per
     * replaceRepoGroup()'s own doc comment), and re-sorts by match
     * count — the same ranking order every other list of RepoIssueGroup
     * in this app maintains.
     */
    fun applyRepoGroupReplacement(
        groups: List<RepoIssueGroup>,
        repoFullName: String,
        updatedGroup: RepoIssueGroup?,
    ): List<RepoIssueGroup> =
        groups.filterNot { it.repoFullName == repoFullName }
            .let { withoutRepo -> if (updatedGroup != null) withoutRepo + updatedGroup else withoutRepo }
            .sortedByDescending { it.matchCount }

    /**
     * Clears EVERY cached (language, mode) combination, not just the
     * active one — a known, deliberate simplification, not an
     * oversight. A per-(language, mode) clear (e.g. a long-press on one
     * entry somewhere) is a natural, cheap follow-up once this is seen
     * in use — the underlying storage is already keyed correctly for
     * it, this just doesn't expose that granularity yet.
     */
    fun clearAll(context: Context) {
        file(context).delete()
    }

    private fun file(context: Context): File = File(context.filesDir, FILE_NAME)

    private fun readAll(context: Context): Map<String, CacheEntry> {
        val f = file(context)
        if (!f.exists()) return emptyMap()
        return try {
            adapter.fromJson(f.readText()) ?: emptyMap()
        } catch (e: Exception) {
            // Corrupt file, or a shape left over from a different cache
            // format — treat as empty rather than crash the screen that
            // was just trying to load quickly.
            emptyMap()
        }
    }

    private fun writeAll(context: Context, all: Map<String, CacheEntry>) {
        try {
            file(context).writeText(adapter.toJson(all))
        } catch (e: Exception) {
            // Best-effort — a failed cache write shouldn't crash or
            // block the screen; the data the user is looking at is
            // already in memory regardless of whether this persisted.
        }
    }
}
