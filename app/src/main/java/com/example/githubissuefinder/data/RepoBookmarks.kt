package com.example.githubissuefinder.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.githubissuefinder.model.Issue
import com.example.githubissuefinder.network.GitHubApiClient
import com.squareup.moshi.Types
import java.io.File

/**
 * Persisted bookmarks for the repo-first Discover flow — a plain JSON
 * file in app-private storage, same overall shape as
 * RepoIssueGroupCache, but for a full issue an the user explicitly
 * chose to save rather than a cached search result.
 *
 * A JSON file, not SharedPreferences — matching RepoIssueGroupCache's
 * reasoning, not IssueModeRepository's: a bookmark is a full Issue object (title,
 * labels, repo identity, URL...), not a single primitive value, so it
 * needs real serialization the way RepoIssueGroupCache's cached search
 * results already do.
 *
 * Compose-reactive by design: `bookmarkedIds` is backed by
 * mutableStateOf, so any Composable reading isBookmarked(id) or
 * all() recomposes automatically the instant a bookmark is toggled
 * anywhere in the app — no manual refresh, no passing a fresh list
 * down through navigation. The actual Issue objects live in a plain
 * (non-reactive) in-memory map alongside it; only the ID set needs to
 * be observable, since that's the only thing UI reads to decide
 * whether to show a filled or empty star.
 *
 * init(context) mirrors IssueModeRepository's and RepoIssueGroupCache's own
 * setup — called once from MainActivity.onCreate, before setContent,
 * so bookmarks are already loaded by the time any screen needs them.
 * toggle() updates the in-memory state SYNCHRONOUSLY (so the star
 * icon and any snackbar message reflect the change instantly, with no
 * flicker or delay) and then persists to disk on Dispatchers.IO
 * afterward — the write never blocks the UI thread, and a failed
 * write doesn't roll back the in-memory toggle the user already saw
 * happen (matching RepoIssueGroupCache's own "best-effort persistence,
 * never crash the screen over it" stance).
 */
object RepoBookmarks {

    private const val FILE_NAME = "repo_bookmarks.json"

    private var appContext: Context? = null
    private val issuesById = mutableMapOf<Long, Issue>()

    /** The reactive half of this store — read this (not issuesById directly) from any Composable that needs to recompose on change. */
    var bookmarkedIds by mutableStateOf<Set<Long>>(emptySet())
        private set

    private val listType = Types.newParameterizedType(List::class.java, Issue::class.java)
    private val adapter by lazy { GitHubApiClient.moshi.adapter<List<Issue>>(listType) }

    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        val loaded = readAll()
        issuesById.putAll(loaded.associateBy { it.id })
        bookmarkedIds = issuesById.keys.toSet()
    }

    fun isBookmarked(issueId: Long): Boolean = issueId in bookmarkedIds

    /**
     * Deliberately derives from bookmarkedIds (the reactive
     * mutableStateOf) rather than reading issuesById directly, even
     * though the two are always kept in sync — this is what makes any
     * Composable calling all() correctly recompose when a bookmark is
     * toggled. issuesById itself is a plain, non-Compose-observable
     * map; a function that only read that map would give Compose no
     * State read to subscribe to, and a screen built on it simply
     * wouldn't update. Reading bookmarkedIds as part of computing the
     * result is what establishes that dependency correctly.
     */
    fun all(): List<Issue> = bookmarkedIds.mapNotNull { issuesById[it] }

    /**
     * Toggles the given issue's bookmark state and returns the NEW
     * state (true = now bookmarked) — callers use this return value
     * directly rather than re-reading isBookmarked() right after, to
     * avoid any doubt about which state a caller observes if this were
     * ever called twice in quick succession.
     */
    fun toggle(context: Context, issue: Issue): Boolean {
        init(context) // safe no-op if already initialized; defensive for any call site that hasn't gone through MainActivity's own init() for some reason
        val nowBookmarked: Boolean
        if (issue.id in issuesById) {
            issuesById.remove(issue.id)
            nowBookmarked = false
        } else {
            issuesById[issue.id] = issue
            nowBookmarked = true
        }
        bookmarkedIds = issuesById.keys.toSet()
        persistAsync()
        return nowBookmarked
    }

    private fun persistAsync() {
        // Deliberately NOT suspend/coroutine-based — this object has no
        // CoroutineScope of its own (it's a plain object, not tied to
        // any ViewModel's lifecycle), and giving it one just for this
        // would be more machinery than a single fire-and-forget file
        // write warrants. Kotlin's own Thread is enough here — this is
        // the one place in this codebase's persistence layer that
        // doesn't route through a ViewModel's Dispatchers.IO, because
        // there's deliberately no ViewModel involved in this path at
        // all (see IssueCard's own doc comment for why toggling is
        // fired directly from the Composable).
        Thread {
            val context = appContext ?: return@Thread
            try {
                File(context.filesDir, FILE_NAME).writeText(adapter.toJson(issuesById.values.toList()))
            } catch (e: Exception) {
                // Best-effort — a failed write shouldn't crash or block
                // anything; the in-memory state the user already saw
                // change is unaffected either way.
            }
        }.start()
    }

    private fun readAll(): List<Issue> {
        val context = appContext ?: return emptyList()
        val f = File(context.filesDir, FILE_NAME)
        if (!f.exists()) return emptyList()
        return try {
            adapter.fromJson(f.readText()) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
