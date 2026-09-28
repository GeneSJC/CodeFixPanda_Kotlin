package com.example.githubissuefinder.ui.discover

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.githubissuefinder.model.IssueLabelFilter

private const val PREFS_NAME = "issue_mode_prefs"
private const val KEY_MODE = "mode"

/**
 * The sticky "which kind of issue am I looking for" state at the heart
 * of the Issue Mode paradigm — promoted from a per-load tag chip
 * (the abandoned old v0.42's approach) to app-wide, deliberately-chosen
 * state the whole repo-first Discover flow now runs through.
 *
 * `current` is nullable: null means no mode has been chosen yet.
 * DiscoverNavHost checks this and shows IssueModeGateScreen instead of
 * the normal flow until it's set — "you have to set it into the mode
 * you're looking for" was explicit in how this was asked for, so this
 * is a genuine gate, not a silent default to one mode or the other.
 *
 * v0.44: now persisted via SharedPreferences, restored once via
 * init(context) — before v0.44, every fresh launch dropped back to the
 * gate screen regardless of how many times a mode had already been
 * chosen, which actively undercut the "deliberate, sticky, whole-app
 * choice" framing the paradigm is built on.
 *
 * SharedPreferences, deliberately NOT a JSON file matching
 * RepoIssueGroupCache's pattern: that cache legitimately needs JSON
 * because it stores a genuinely complex nested structure (a map of
 * language+mode keys to lists of RepoIssueGroup, each holding a list
 * of Issue objects). This is a single enum value, or absent. Hand-
 * rolling JSON serialization, a Moshi adapter, and Dispatchers.IO file
 * I/O for one value that's either "GOOD_FIRST_ISSUE", "HELP_WANTED",
 * or nothing would be real, avoidable complexity for what it buys —
 * SharedPreferences is the appropriately-sized tool for exactly this
 * job: built into Android, no new dependency, synchronous reads fast
 * enough to not need Dispatchers.IO at this size, two lines to read
 * and two to write.
 *
 * A plain object holding Compose's own mutableStateOf, same pattern
 * and same reasoning as ui/theme/ThemePreference.kt: the places that
 * need to WRITE this (IssueModeGateScreen; previously also a Top Repos
 * dropdown, since removed) and the places that need to READ it
 * (MainActivity, at the app's literal root, to pick a color scheme;
 * TopReposViewModel, to know which label to query for) aren't all
 * parent/child in the composition tree. A small shared object read
 * directly wherever needed avoids threading a new parameter through
 * DiscoverNavHost and everything under it. Same tradeoff ThemePreference
 * accepted: harder to unit-test in isolation, implicit rather than
 * explicit data flow — reasonable here for the same reason: one value,
 * one job.
 *
 * Deliberately scoped to ui/discover, not somewhere more generically
 * "app-wide" like ThemePreference — Issue Mode is specifically a
 * repo-first Discover flow concept.
 *
 * TopReposViewModel reads `current` directly too (not just Composables)
 * — the same "read a plain shared value from a ViewModel without a
 * live subscription" pattern GitHubApiClient.hasToken already uses
 * elsewhere in this codebase, not a new one introduced here.
 *
 * mutableStateOf<IssueLabelFilter?>(null) — the explicit type argument
 * is required, not stylistic: mutableStateOf(null) alone infers
 * MutableState<Nothing?>, which can only ever hold null and has no
 * setValue that accepts an actual IssueLabelFilter, so `current = mode`
 * anywhere in the codebase would fail to compile with "has no method
 * setValue(...), so it cannot serve as a delegate." A real error caught
 * this way when v0.41 was first applied — worth knowing about since
 * it's an easy mistake to reintroduce in any future nullable
 * mutableStateOf of this shape.
 *
 * getValue/setValue ARE ALSO REQUIRED IMPORTS, separately from the fix
 * above: Kotlin's `by` delegation for MutableState resolves to
 * top-level extension functions Compose provides in this same
 * androidx.compose.runtime package — they aren't members of
 * MutableState itself, so without both imported, `by` fails with the
 * same-looking "has no method getValue(...)/setValue(...), so it
 * cannot serve as a delegate" error, easily mistaken for the type-
 * argument issue above since the message is nearly identical. This is
 * a second, genuinely separate mistake this file had at the same
 * time, not a continuation of the first — caught only once the first
 * fix was actually applied and rebuilt, since the type-argument error
 * masked this missing-import error underneath it. Every other file in
 * this codebase using `by mutableStateOf(...)`/`by remember { ... }`
 * already imports both (see e.g. TopReposScreen.kt) — this file simply
 * hadn't, being new.
 */
object IssueMode {

    private var appContext: Context? = null
    private var _current by mutableStateOf<IssueLabelFilter?>(null)

    var current: IssueLabelFilter?
        get() = _current
        set(value) {
            _current = value
            appContext?.let { context ->
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_MODE, value?.name)
                    .apply()
            }
        }

    /**
     * Restores whatever mode was last saved, if any — called once from
     * MainActivity.onCreate, BEFORE setContent, so the restored value
     * (or lack of one) is already in place by the time DiscoverNavHost
     * computes its startDestination on first composition. Safe to call
     * more than once (e.g. if MainActivity is ever recreated) — it
     * only restores from disk if this object doesn't already have a
     * live in-memory value AND hasn't been initialized with a context
     * yet, so it never clobbers a mode the user already picked this
     * process with whatever was on disk before that.
     *
     * Takes the Application context specifically (via
     * context.applicationContext), not the Activity, since this object
     * outlives any single Activity instance for the life of the
     * process — the same reasoning TopReposViewModel's switch to
     * AndroidViewModel already relies on via getApplication().
     */
    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        val savedName = appContext
            ?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.getString(KEY_MODE, null)
        _current = savedName?.let { name -> IssueLabelFilter.entries.find { it.name == name } }
    }
}
