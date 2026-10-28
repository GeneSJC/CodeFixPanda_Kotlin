package io.github.genesjc.opensourcepanda.data

import android.content.Context

private const val PREFS_NAME = "startup_splash_prefs"
private const val KEY_FULL_ANIMATION_SEEN = "full_animation_seen"

/**
 * Where "has this device already seen the full start animation" is
 * persisted, split out from StartupSplashRepository so the repository
 * can be unit-tested against a plain in-memory fake with no Android
 * Context — same shape as IssueModeStorage.
 */
interface StartupSplashStorage {
    fun hasSeenFullAnimation(): Boolean
    fun markFullAnimationSeen()
}

/**
 * SharedPreferences for the same reason IssueModeRepository uses it: a
 * single boolean doesn't justify a JSON file or Dispatchers.IO, and
 * `.apply()` makes the write itself asynchronous.
 */
class SharedPreferencesStartupSplashStorage(context: Context) : StartupSplashStorage {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun hasSeenFullAnimation(): Boolean = prefs.getBoolean(KEY_FULL_ANIMATION_SEEN, false)

    override fun markFullAnimationSeen() {
        prefs.edit().putBoolean(KEY_FULL_ANIMATION_SEEN, true).apply()
    }
}

/**
 * The single owner of whether the full title-reveal animation has already played on this device:
 * the first launch plays it in full and later launches show the short version. Plain methods and not a
 * StateFlow: the decision is read once per launch, before the splash starts, and must not flip
 * mid-animation when the flag is written at the end. The in-app "Play App Start Animation" replay
 * never touches it.
 */
class StartupSplashRepository(private val storage: StartupSplashStorage) {

    val hasSeenFullAnimation: Boolean
        get() = storage.hasSeenFullAnimation()

    fun markFullAnimationSeen() {
        storage.markFullAnimationSeen()
    }
}
