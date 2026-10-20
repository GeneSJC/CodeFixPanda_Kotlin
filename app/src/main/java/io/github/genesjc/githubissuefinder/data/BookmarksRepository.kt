package io.github.genesjc.githubissuefinder.data

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import io.github.genesjc.githubissuefinder.model.Issue
import java.io.File
import java.util.concurrent.Executors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val FILE_NAME = "repo_bookmarks.json"

/**
 * Where bookmarks are persisted, split out from BookmarksRepository so
 * the repository's logic can be unit-tested against a plain in-memory
 * fake with no Android Context — same shape as HiddenIssuesStorage.
 */
interface BookmarksStorage {
    /** Every saved bookmark, in saved order, or an empty list if there are none (or the file is unreadable). */
    fun load(): List<Issue>

    /** Replaces the saved list with [issues]. Best-effort: a failed write never throws. */
    fun save(issues: List<Issue>)
}

/**
 * A JSON file in private storage: a bookmark is a full Issue, not a primitive, so it needs real
 * serialization. The file name is unchanged, so earlier bookmarks survive. Writes run in order on
 * one background thread; a failed write is swallowed on purpose and never rolls back a toggle
 * the user already saw.
 */
class JsonFileBookmarksStorage(context: Context, moshi: Moshi) : BookmarksStorage {

    private val file = File(context.applicationContext.filesDir, FILE_NAME)
    private val writer = Executors.newSingleThreadExecutor()
    private val adapter by lazy {
        moshi.adapter<List<Issue>>(
            Types.newParameterizedType(List::class.java, Issue::class.java),
        )
    }

    override fun load(): List<Issue> {
        if (!file.exists()) return emptyList()
        return try {
            adapter.fromJson(file.readText()) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun save(issues: List<Issue>) {
        writer.execute {
            try {
                file.writeText(adapter.toJson(issues))
            } catch (e: Exception) {
                // Best-effort — see the class doc comment.
            }
        }
    }
}

/**
 * The single owner of the issues the user has bookmarked (full Issue objects, not cached search
 * results). [bookmarkedIds] draws each card's star and [bookmarks] feeds the Bookmarks screen;
 * both are read-only views of one source, updated together in [publish]. [toggle] updates memory
 * immediately, so the star and Snackbar reflect it at once, and persists afterward on a
 * background thread.
 */
class BookmarksRepository(private val storage: BookmarksStorage) {

    /** Insertion order is preserved: bookmarks are listed in the order they were saved. */
    private var issuesById: Map<Long, Issue> = storage.load().associateBy { it.id }

    private val _bookmarkedIds = MutableStateFlow(issuesById.keys.toSet())

    /** The ids of bookmarked issues — what a list screen reads to draw each star. */
    val bookmarkedIds: StateFlow<Set<Long>> = _bookmarkedIds.asStateFlow()

    private val _bookmarks = MutableStateFlow(issuesById.values.toList())

    /** Every bookmarked issue, in the order they were saved — the Bookmarks screen's list. */
    val bookmarks: StateFlow<List<Issue>> = _bookmarks.asStateFlow()

    /**
     * Toggles [issue]'s bookmark state and returns the NEW state (true =
     * now bookmarked), so a caller can use the result directly rather
     * than re-reading it afterward.
     */
    fun toggle(issue: Issue): Boolean {
        val nowBookmarked = issue.id !in issuesById
        publish(if (nowBookmarked) issuesById + (issue.id to issue) else issuesById - issue.id)
        return nowBookmarked
    }

    /** The one place state changes: update every view of it, then save. */
    private fun publish(newState: Map<Long, Issue>) {
        issuesById = newState
        _bookmarkedIds.value = newState.keys.toSet()
        _bookmarks.value = newState.values.toList()
        storage.save(newState.values.toList())
    }
}
