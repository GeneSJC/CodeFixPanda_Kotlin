package io.github.genesjc.opensourcepanda.data

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import io.github.genesjc.opensourcepanda.model.IssueLabelFilter
import io.github.genesjc.opensourcepanda.model.LanguageFilter
import io.github.genesjc.opensourcepanda.model.RepoIssueGroup
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val FILE_NAME = "repo_issue_group_cache.json"
private const val TAG = "RepoIssueGroupStorage"

/**
 * One cached result: the groups Top Repos had loaded for a (language, mode) pair, plus how far
 * Load More had got, so restoring it puts Top Repos back where it was and not at page 1.
 */
data class RepoIssueGroupCacheEntry(
    val repoGroups: List<RepoIssueGroup>,
    val totalIssueCount: Int,
    val currentPage: Int,
    val canLoadMore: Boolean,
    val fetchedAtMs: Long,
)

/** The cache key for one (language, mode) combination. */
fun cacheKeyFor(language: LanguageFilter, mode: IssueLabelFilter): String = "${language.name}|${mode.name}"

/**
 * Where the cache is stored, split out so the repository's logic can be
 * unit-tested against a plain in-memory fake with no Android Context.
 * All three calls are blocking; the repository runs them off the main
 * thread.
 */
interface RepoIssueGroupStorage {
    /** Every saved entry by key, or an empty map if there is none (or the file is unreadable). */
    fun readAll(): Map<String, RepoIssueGroupCacheEntry>

    /** Replaces everything saved. Best-effort: a failed write never throws. */
    fun writeAll(all: Map<String, RepoIssueGroupCacheEntry>)

    /** Deletes everything saved. */
    fun clear()
}

/**
 * A plain JSON file in app-private storage — ONE combined file (not one
 * per key): a single read/write per operation is simpler to reason about
 * and cheap at this size (a handful of language/mode combinations). A
 * JSON file and not Room, the same "avoid new persistence
 * infrastructure for a small, single-purpose cache" choice made before.
 * Takes the app-wide Moshi rather than building a second one.
 */
class JsonFileRepoIssueGroupStorage(context: Context, moshi: Moshi) : RepoIssueGroupStorage {

    private val file = File(context.applicationContext.filesDir, FILE_NAME)
    private val adapter by lazy {
        moshi.adapter<Map<String, RepoIssueGroupCacheEntry>>(
            Types.newParameterizedType(Map::class.java, String::class.java, RepoIssueGroupCacheEntry::class.java),
        )
    }

    override fun readAll(): Map<String, RepoIssueGroupCacheEntry> {
        if (!file.exists()) return emptyMap()
        return try {
            adapter.fromJson(file.readText()) ?: emptyMap()
        } catch (e: Exception) {
            // A cache that cannot be read is just a miss: the list is fetched again.
            Log.w(TAG, "The cache could not be read; starting empty", e)
            emptyMap()
        }
    }

    override fun writeAll(all: Map<String, RepoIssueGroupCacheEntry>) {
        try {
            file.writeText(adapter.toJson(all))
        } catch (e: Exception) {
            // The cache is rebuilt from a fresh search, so a failed write costs one refetch, not the user's data.
            Log.w(TAG, "The cache could not be saved", e)
        }
    }

    override fun clear() {
        file.delete()
    }
}

/**
 * The single owner of Top Repos' persisted results, keyed by (language, mode): returning to a
 * combination already visited is instant and costs no Search API calls. Cache-first and never
 * auto-refreshed, since a background refresh would spend the calls this cache exists to save;
 * fresh data is an explicit choice (see TopReposStateHolder.checkForNewRepos()). File work runs
 * on [ioDispatcher], so a caller just calls a suspend function. The mutex guards the
 * read-modify-write over the one combined file, so overlapping calls cannot overwrite each other.
 */
class RepoIssueGroupRepository(
    private val storage: RepoIssueGroupStorage,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    private val lock = Mutex()

    suspend fun load(language: LanguageFilter, mode: IssueLabelFilter): RepoIssueGroupCacheEntry? =
        withContext(ioDispatcher) {
            lock.withLock { storage.readAll()[cacheKeyFor(language, mode)] }
        }

    suspend fun save(
        language: LanguageFilter,
        mode: IssueLabelFilter,
        repoGroups: List<RepoIssueGroup>,
        totalIssueCount: Int,
        currentPage: Int,
        canLoadMore: Boolean,
        fetchedAtMs: Long = clock(),
    ) {
        withContext(ioDispatcher) {
            lock.withLock {
                val updated = storage.readAll().toMutableMap()
                updated[cacheKeyFor(language, mode)] = RepoIssueGroupCacheEntry(
                    repoGroups = repoGroups,
                    totalIssueCount = totalIssueCount,
                    currentPage = currentPage,
                    canLoadMore = canLoadMore,
                    fetchedAtMs = fetchedAtMs,
                )
                storage.writeAll(updated)
            }
        }
    }

    /**
     * v0.50 ("Refresh this repo" write-back): writes ONE repo's refreshed
     * group back into the entry it came from, so Top Repos shows the
     * updated match count (or drops the repo, if it has none left)
     * without a second network call. Keeps the entry's original
     * fetchedAtMs on purpose: a single-repo refresh is not a list-level
     * fetch and shouldn't make the whole list look fresher than it is.
     * Does nothing if there is no entry for this (language, mode).
     */
    suspend fun replaceRepoGroup(
        language: LanguageFilter,
        mode: IssueLabelFilter,
        repoFullName: String,
        updatedGroup: RepoIssueGroup?,
    ) {
        withContext(ioDispatcher) {
            lock.withLock {
                val all = storage.readAll()
                val key = cacheKeyFor(language, mode)
                val entry = all[key] ?: return@withLock
                val updated = all.toMutableMap()
                updated[key] = entry.copy(repoGroups = applyRepoGroupReplacement(entry.repoGroups, repoFullName, updatedGroup))
                storage.writeAll(updated)
            }
        }
    }

    /**
     * Clears the ENTIRE cache, not just the active (language, mode)
     * pair: the Clear Cache menu item means "start over."
     */
    suspend fun clearAll() {
        withContext(ioDispatcher) {
            lock.withLock { storage.clear() }
        }
    }
}

/**
 * The pure list-transform behind replaceRepoGroup(): drops the repo's
 * old group, adds [updatedGroup] if there is one (null removes the repo —
 * the "zero open issues left" case), and re-sorts by match count. Every
 * OTHER repo's group is left untouched. A top-level function so it is
 * directly unit-testable.
 */
fun applyRepoGroupReplacement(
    groups: List<RepoIssueGroup>,
    repoFullName: String,
    updatedGroup: RepoIssueGroup?,
): List<RepoIssueGroup> =
    groups.filterNot { it.repoFullName == repoFullName }
        .let { withoutRepo -> if (updatedGroup != null) withoutRepo + updatedGroup else withoutRepo }
        .sortedByDescending { it.matchCount }
