package io.github.genesjc.githubissuefinder.data

import android.content.Context
import io.github.genesjc.githubissuefinder.model.IssueLabelFilter
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
 * SharedPreferences, not a JSON file: this is one enum value or nothing, and a file would be
 * avoidable complexity. It is built in, synchronous reads are fast enough to skip Dispatchers.IO,
 * and apply() makes the write asynchronous.
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
 * The single owner of the app-wide Issue Mode, the sticky choice at the heart of the Discover
 * flow. ViewModels ask it to change the mode and observe [mode], a StateFlow so ViewModels can
 * read it without Compose; only this class can change it. Null means no mode has been chosen,
 * and DiscoverNavHost shows the gate until it is set.
 *
 * The stored value is read once at construction, ahead of the first composition, so the start
 * destination is right on launch. A stored name that no longer matches any IssueLabelFilter
 * restores as null and shows the gate again instead of crashing.
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
