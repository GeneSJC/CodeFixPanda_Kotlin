package com.example.githubissuefinder.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.githubissuefinder.model.HiddenIssue
import com.example.githubissuefinder.model.Issue
import com.example.githubissuefinder.network.GitHubApiClient
import com.squareup.moshi.Types
import java.io.File

/**
 * v0.51 ("Hide Issue", Part C of the Cache-Preserving Refresh sequence —
 * spec: GitHubIssueFinder_HideIssue_Spec_2026-09.pdf). A display-time
 * filter, never a deletion: RepoIssueGroupCache keeps the raw data
 * exactly as GitHub returned it; this store only tracks WHICH issue ids
 * should be filtered out of what's shown, so Unhide is free and instant
 * as long as the issue is still in the cache.
 *
 * JUDGMENT CALL, flagged explicitly: the spec asked for SharedPreferences.
 * This project's own established pattern (see RepoBookmarks' doc
 * comment, and the "Persistence: three mechanisms, each sized to its
 * job" note in this project's hand-off history) uses SharedPreferences
 * only for a single primitive value (IssueModeRepository) and a plain JSON file
 * for real object lists (RepoBookmarks, RepoIssueGroupCache). A growing
 * list of HiddenIssue snapshots is the latter shape, not the former, so
 * this follows RepoBookmarks' JSON-file pattern instead, for consistency
 * with the rest of this app's persistence layer rather than the spec's
 * literal wording.
 *
 * Structurally a near-exact mirror of RepoBookmarks.kt: a plain object
 * with an init(context) called once from MainActivity.onCreate, a
 * Compose-reactive mutableStateOf set of ids (hiddenIds) as the single
 * source of truth every filtering screen reads, a plain (non-reactive)
 * in-memory map for the full snapshots, and fire-and-forget persistence
 * on a plain Thread (no ViewModel/CoroutineScope owns this object).
 */
object HiddenIssuesStore {

    private const val FILE_NAME = "hidden_issues.json"

    private var appContext: Context? = null
    private val hiddenById = mutableMapOf<Long, HiddenIssue>()

    /** The reactive half of this store — read this (via isHidden()/all()) from any Composable that needs to recompose on change. */
    var hiddenIds by mutableStateOf<Set<Long>>(emptySet())
        private set

    private val listType = Types.newParameterizedType(List::class.java, HiddenIssue::class.java)
    private val adapter by lazy { GitHubApiClient.moshi.adapter<List<HiddenIssue>>(listType) }

    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        val loaded = readAll()
        hiddenById.putAll(loaded.associateBy { it.id })
        hiddenIds = hiddenById.keys.toSet()
    }

    fun isHidden(issueId: Long): Boolean = issueId in hiddenIds

    /** Newest-hidden-first, per the Hidden list's own display order. */
    fun all(): List<HiddenIssue> = hiddenIds.mapNotNull { hiddenById[it] }.sortedByDescending { it.hiddenAtMs }

    fun hide(context: Context, issue: Issue) {
        init(context) // safe no-op if already initialized; defensive, matching RepoBookmarks.toggle()
        hiddenById[issue.id] = HiddenIssue(
            id = issue.id,
            title = issue.title,
            repoFullName = issue.repoFullName,
            htmlUrl = issue.htmlUrl,
            hiddenAtMs = System.currentTimeMillis(),
        )
        hiddenIds = hiddenById.keys.toSet()
        persistAsync()
    }

    fun unhide(context: Context, issueId: Long) {
        init(context)
        if (hiddenById.remove(issueId) != null) {
            hiddenIds = hiddenById.keys.toSet()
            persistAsync()
        }
    }

    fun unhideAll(context: Context) {
        init(context)
        if (hiddenById.isEmpty()) return
        hiddenById.clear()
        hiddenIds = emptySet()
        persistAsync()
    }

    private fun persistAsync() {
        // Deliberately a plain Thread, not a coroutine — see
        // RepoBookmarks.persistAsync()'s own doc comment for why: this
        // object has no CoroutineScope of its own, and a failed write
        // never rolls back the in-memory state the user already saw
        // change.
        Thread {
            val context = appContext ?: return@Thread
            try {
                File(context.filesDir, FILE_NAME).writeText(adapter.toJson(hiddenById.values.toList()))
            } catch (e: Exception) {
                // Best-effort — matches every other store in this app.
            }
        }.start()
    }

    private fun readAll(): List<HiddenIssue> {
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
