package com.example.githubissuefinder.data

import android.content.Context
import com.example.githubissuefinder.model.IssueLabelFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val PREFS_NAME = "issue_mode_prefs"
private const val KEY_MODE = "mode"

/**
 * Where the chosen Issue Mode is persisted, split out from
 * IssueModeRepository so the repository's own logic (restore on
 * construction, write on change, ignore an unrecognized stored value)
 * can be unit-tested with a plain in-memory fake — no Android Context
 * needed. The one real implementation is
 * SharedPreferencesIssueModeStorage below.
 */
interface IssueModeStorage {
    /** The stored enum name, or null if nothing has been saved (or it was cleared). */
    fun load(): String?

    /** Saves [name], or clears the stored value when null. */
    fun save(name: String?)
}

/**
 * SharedPreferences, deliberately NOT a JSON file matching
 * RepoIssueGroupRepository's pattern: that cache legitimately needs JSON
 * because it stores a genuinely complex nested structure. This is a
 * single enum value, or absent. Hand-rolling serialization and
 * Dispatchers.IO file I/O for one value that's either
 * "GOOD_FIRST_ISSUE", "HELP_WANTED", or nothing would be real,
 * avoidable complexity — SharedPreferences is the appropriately-sized
 * tool: built into Android, no new dependency, synchronous reads fast
 * enough to not need Dispatchers.IO at this size, and `.apply()` makes
 * the write itself asynchronous.
 */
class SharedPreferencesIssueModeStorage(context: Context) : IssueModeStorage {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun load(): String? = prefs.getString(KEY_MODE, null)

    override fun save(name: String?) {
        prefs.edit().putString(KEY_MODE, name).apply()
    }
}

/**
 * The single owner of "which kind of issue am I looking for" — the
 * sticky, app-wide Issue Mode at the heart of the repo-first Discover
 * flow. Replaces the old ui/discover/IssueMode object, which exposed a
 * Compose mutableStateOf directly and wrote to SharedPreferences from
 * inside its property setter, meaning UI code (a navigation callback)
 * indirectly decided when a disk write happened, and every screen read
 * a global without declaring that it depended on it.
 *
 * Why a repository, in the data layer: persistence is a data-layer
 * concern. ViewModels ask this class to change the mode and observe it;
 * nothing in the UI knows or cares that it is backed by
 * SharedPreferences, or when it is written. Swapping the storage — or
 * faking it in a test — touches this file's constructor argument and
 * nothing else.
 *
 * Why [mode] is a StateFlow and not Compose state: a StateFlow has no
 * dependency on Compose, so ViewModels (which have no Compose
 * runtime of their own) can read and observe it the same way a
 * composable can, via collectAsStateWithLifecycle(). The private
 * [_mode] / public [mode] pair is the standard encapsulation pattern
 * (see also IssueSearchRepository.rateLimit): only this class can change the value, and
 * everyone else gets a read-only view of it.
 *
 * Null means no mode has been chosen yet. DiscoverNavHost shows
 * IssueModeGateScreen instead of the normal flow until it's set — "you
 * have to set it into the mode you're looking for" was explicit in how
 * this was asked for, so this is a genuine gate, not a silent default.
 *
 * The stored value is read once, when the repository is constructed —
 * which happens when the first ViewModel that needs it is created, ahead
 * of the first composition that reads it, so the restored value (or lack
 * of one) is already in place by the time DiscoverNavHost computes its
 * start destination. A stored name that no longer matches any
 * IssueLabelFilter (e.g. one renamed in a later version) restores as
 * null, which just shows the gate again rather than crashing.
 *
 * Scope: Issue Mode is specifically a repo-first Discover flow concept,
 * which is why this lives alongside the other Discover-flow stores in
 * data/ rather than somewhere more generically app-wide.
 */
class IssueModeRepository(private val storage: IssueModeStorage) {

    private val _mode = MutableStateFlow(restore())
    val mode: StateFlow<IssueLabelFilter?> = _mode.asStateFlow()

    /** Sets the mode (or clears it with null) and persists the change. */
    fun setMode(newMode: IssueLabelFilter?) {
        _mode.value = newMode
        storage.save(newMode?.name)
    }

    private fun restore(): IssueLabelFilter? {
        val savedName = storage.load() ?: return null
        return IssueLabelFilter.entries.find { it.name == savedName }
    }
}
