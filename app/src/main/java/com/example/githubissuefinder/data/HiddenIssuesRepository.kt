package com.example.githubissuefinder.data

import android.content.Context
import com.example.githubissuefinder.model.HiddenIssue
import com.example.githubissuefinder.model.Issue
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.io.File
import java.util.concurrent.Executors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val FILE_NAME = "hidden_issues.json"

/**
 * Where the hidden issues are persisted, split out from
 * HiddenIssuesRepository so the repository's logic can be unit-tested
 * against a plain in-memory fake with no Android Context — same shape
 * as IssueModeStorage and ApiTokenStorage.
 */
interface HiddenIssuesStorage {
    /** Every saved hidden issue, or an empty list if there are none (or the file is unreadable). */
    fun load(): List<HiddenIssue>

    /** Replaces the saved list with [issues]. Best-effort: a failed write never throws. */
    fun save(issues: List<HiddenIssue>)
}

/**
 * A JSON file in private storage, the project's pattern for lists of objects (SharedPreferences
 * is for single values). The file name is unchanged, so issues hidden before the refactor are
 * still hidden. Writes run in order on one background thread, so two quick changes cannot land
 * out of order. A failed write is swallowed on purpose: it never rolls back what the user already
 * saw, like every other store.
 */
class JsonFileHiddenIssuesStorage(context: Context, moshi: Moshi) : HiddenIssuesStorage {

    private val file = File(context.applicationContext.filesDir, FILE_NAME)
    private val writer = Executors.newSingleThreadExecutor()
    private val adapter by lazy {
        moshi.adapter<List<HiddenIssue>>(
            Types.newParameterizedType(List::class.java, HiddenIssue::class.java),
        )
    }

    override fun load(): List<HiddenIssue> {
        if (!file.exists()) return emptyList()
        return try {
            adapter.fromJson(file.readText()) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun save(issues: List<HiddenIssue>) {
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
 * The single owner of which issues the user has hidden. Hiding is a display-time filter, never a
 * deletion: the cached data stays as GitHub returned it and this tracks which ids to filter out,
 * so Unhide is instant while the issue is still cached. [hiddenIds] and [hiddenIssues] (newest
 * first) are read-only views of one source, updated together in [publish], so they cannot
 * disagree with each other or with what is saved. [clock] is a parameter so a test can control
 * "when it was hidden".
 */
class HiddenIssuesRepository(
    private val storage: HiddenIssuesStorage,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    private var hiddenById: Map<Long, HiddenIssue> = storage.load().associateBy { it.id }

    private val _hiddenIds = MutableStateFlow(hiddenById.keys.toSet())

    /** The ids to filter out of any list being shown. */
    val hiddenIds: StateFlow<Set<Long>> = _hiddenIds.asStateFlow()

    private val _hiddenIssues = MutableStateFlow(sortedNewestFirst(hiddenById))

    /** Every hidden issue, most recently hidden first — the Hidden screen's list. */
    val hiddenIssues: StateFlow<List<HiddenIssue>> = _hiddenIssues.asStateFlow()

    /** Hides [issue], keeping a small snapshot of it so it can be listed (and unhidden) even if it leaves the cache. */
    fun hide(issue: Issue) {
        val snapshot = HiddenIssue(
            id = issue.id,
            title = issue.title,
            repoFullName = issue.repoFullName,
            htmlUrl = issue.htmlUrl,
            hiddenAtMs = clock(),
            tag = issue.tag,
        )
        publish(hiddenById + (issue.id to snapshot))
    }

    fun unhide(issueId: Long) {
        if (issueId !in hiddenById) return
        publish(hiddenById - issueId)
    }

    /** Unhides every hidden issue carrying [tag] (null for real data), leaving other sources' entries alone. */
    fun unhideAll(tag: String?) {
        val remaining = hiddenById.filterValues { it.tag != tag }
        if (remaining.size == hiddenById.size) return
        publish(remaining)
    }

    /** The one place state changes: update every view of it, then save. */
    private fun publish(newState: Map<Long, HiddenIssue>) {
        hiddenById = newState
        _hiddenIds.value = newState.keys.toSet()
        _hiddenIssues.value = sortedNewestFirst(newState)
        storage.save(newState.values.toList())
    }

    private fun sortedNewestFirst(byId: Map<Long, HiddenIssue>): List<HiddenIssue> =
        byId.values.sortedByDescending { it.hiddenAtMs }
}
