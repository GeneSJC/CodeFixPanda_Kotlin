package io.github.genesjc.opensourcepanda.data

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import io.github.genesjc.opensourcepanda.model.DataOrigin
import io.github.genesjc.opensourcepanda.model.HiddenIssue
import io.github.genesjc.opensourcepanda.model.Issue
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.io.File
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

    /** Replaces the saved list with [issues] in the background. A failed write never throws; it calls [onFailure]. */
    fun save(issues: List<HiddenIssue>, onFailure: () -> Unit)
}

/**
 * A JSON file in private storage, the project's pattern for lists of objects (SharedPreferences
 * is for single values). The file name is unchanged, so issues hidden before the refactor are
 * still hidden. Reading and writing go through [JsonListFile], which writes atomically, in order,
 * on one background thread, so two quick changes cannot land out of order.
 */
class JsonFileHiddenIssuesStorage(context: Context, moshi: Moshi) : HiddenIssuesStorage {

    private val list = JsonListFile(
        file = File(context.applicationContext.filesDir, FILE_NAME),
        adapterFactory = {
            moshi.adapter<List<HiddenIssue>>(Types.newParameterizedType(List::class.java, HiddenIssue::class.java))
        },
    )

    override fun load(): List<HiddenIssue> = list.load()

    override fun save(issues: List<HiddenIssue>, onFailure: () -> Unit) = list.save(issues, onFailure)
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
            origin = issue.origin,
        )
        publish(hiddenById + (issue.id to snapshot))
    }

    fun unhide(issueId: Long) {
        if (issueId !in hiddenById) return
        publish(hiddenById - issueId)
    }

    /** Unhides every hidden issue from [origin], leaving the other origin's entries alone. */
    fun unhideAll(origin: DataOrigin) {
        val remaining = hiddenById.filterValues { it.origin != origin }
        if (remaining.size == hiddenById.size) return
        publish(remaining)
    }

    private val saveFailureChannel = Channel<Unit>(Channel.BUFFERED)

    /**
     * Emits each time a save to disk fails, so a screen can tell the user. A failure is held until something
     * collects it, so one that happens after the screen has gone is still reported by the next screen that
     * listens. The change stays in memory for this session; it is only lost if the app closes before a later
     * save succeeds.
     */
    val saveFailures: Flow<Unit> = saveFailureChannel.receiveAsFlow()

    /** The one place state changes: update every view of it, then save. */
    private fun publish(newState: Map<Long, HiddenIssue>) {
        hiddenById = newState
        _hiddenIds.value = newState.keys.toSet()
        _hiddenIssues.value = sortedNewestFirst(newState)
        storage.save(newState.values.toList()) { saveFailureChannel.trySend(Unit) }
    }

    private fun sortedNewestFirst(byId: Map<Long, HiddenIssue>): List<HiddenIssue> =
        byId.values.sortedByDescending { it.hiddenAtMs }
}
