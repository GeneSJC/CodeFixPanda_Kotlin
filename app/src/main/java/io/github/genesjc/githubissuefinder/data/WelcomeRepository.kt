package io.github.genesjc.githubissuefinder.data

import android.content.Context

private const val PREFS_NAME = "welcome_prefs"
private const val KEY_WELCOME_SEEN = "welcome_seen"

/**
 * Where "has this device already been through the welcome screen" is persisted, split out from
 * WelcomeRepository so the repository can be unit-tested against a plain in-memory fake with no
 * Android Context, the same shape as StartupSplashStorage.
 */
interface WelcomeStorage {
    fun hasSeenWelcome(): Boolean
    fun markWelcomeSeen()
}

/**
 * SharedPreferences, for the same reason as the other one-flag stores: a single boolean does not
 * justify a file. The file name is listed in res/xml/backup_rules.xml and data_extraction_rules.xml so a
 * restored backup on a new device shows the welcome again; keep them in sync if it changes.
 */
class SharedPreferencesWelcomeStorage(context: Context) : WelcomeStorage {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun hasSeenWelcome(): Boolean = prefs.getBoolean(KEY_WELCOME_SEEN, false)

    override fun markWelcomeSeen() {
        prefs.edit().putBoolean(KEY_WELCOME_SEEN, true).apply()
    }
}

/**
 * The single owner of whether the welcome screen has been shown on this device. Plain members and not
 * a StateFlow: the decision is read once, when the app picks its start screen, and must not flip while
 * the welcome is still on screen. It is marked when the user leaves the welcome by any route (a saved
 * token, "Try the demo", or Skip).
 */
class WelcomeRepository(private val storage: WelcomeStorage) {

    val hasSeenWelcome: Boolean
        get() = storage.hasSeenWelcome()

    fun markWelcomeSeen() {
        storage.markWelcomeSeen()
    }
}

/**
 * True when the app must start on the welcome screen: the user has no token (so would only get demo
 * data) and has not been through the welcome yet. A token that is already set, from a developer's local
 * properties or entered earlier, skips it.
 */
fun needsWelcome(hasToken: Boolean, welcomeSeen: Boolean): Boolean = !hasToken && !welcomeSeen
