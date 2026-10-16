package com.example.githubissuefinder.data

import android.content.Context
import com.example.githubissuefinder.model.Issue
import com.example.githubissuefinder.network.GitHubApiClient
import com.squareup.moshi.Types
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
 * A plain JSON file in the app's private storage: a bookmark is a full
 * Issue (title, labels, repo identity, URL...), not a single primitive
 * value, so it needs real serialization the way RepoIssueGroupCache's
 * cached search results do, rather than SharedPreferences. The file
 * name is unchanged from the old RepoBookmarks object, so bookmarks a
 * user already saved are still there after this refactor.
 *
 * Writes happen on a single background thread, in order, so two quick
 * toggles can't land on disk out of order. A failed write is swallowed
 * on purpose: it never rolls back the toggle the user already saw
 * happen, matching every other store in this app.
 */
class JsonFileBookmarksStorage(context: Context) : BookmarksStorage {

    private val file = File(context.applicationContext.filesDir, FILE_NAME)
    private val writer = Executors.newSingleThreadExecutor()
    private val adapter by lazy {
        GitHubApiClient.moshi.adapter<List<Issue>>(
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
 * The single owner of which issues the user has bookmarked: the
 * persisted set of full Issue objects the user explicitly chose to save
 * (as opposed to a cached search result).
 *
 * Replaces the old RepoBookmarks object, which held Compose state and
 * was called directly, with a Context, from two screens. Persistence is
 * now a data-layer concern: ViewModels ask this class to toggle a
 * bookmark and observe the result, and no screen knows how or when
 * anything is written.
 *
 * Why two StateFlows, updated together in one place ([publish]): the
 * issue list screens only need [bookmarkedIds] to decide whether to
 * show a filled or empty star, while the Bookmarks screen needs the
 * full [bookmarks] list. Keeping both as read-only views of one source
 * means they can never disagree with each other or with what is saved.
 * The private mutable / public read-only pairs are the standard
 * encapsulation — only this class can change them.
 *
 * [toggle] updates the in-memory state immediately (so the star and
 * any Snackbar reflect the change with no delay) and persists
 * afterward on a background thread.
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

    companion object {
        @Volatile
        private var _instance: BookmarksRepository? = null

        /**
         * The app-wide instance, same plain-singleton-with-init(context)
         * pattern as the other repositories (this project has no DI
         * framework yet). ViewModels take the repository as a
         * constructor argument defaulting to this so tests can pass
         * their own.
         */
        val instance: BookmarksRepository
            get() = checkNotNull(_instance) {
                "BookmarksRepository.init(context) must be called before use " +
                    "(MainActivity.onCreate does this before setContent)."
            }

        /** Safe to call more than once: only the first call constructs the instance. */
        fun init(context: Context) {
            if (_instance != null) return
            _instance = BookmarksRepository(JsonFileBookmarksStorage(context))
        }
    }
}
