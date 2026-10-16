package com.example.githubissuefinder.data

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
 * The single owner of whether the full title-reveal animation has
 * already played on this device. The first launch plays it in full;
 * every launch after that shows a short fade-in of the app name instead,
 * because the full animation is fun once and a bit much on the second
 * and third viewing.
 *
 * Why a repository and not a bare SharedPreferences call in the splash
 * code: persistence is a data-layer concern, and keeping it behind this
 * class means the ViewModel can be unit-tested with a fake and the UI
 * never decides when a disk write happens.
 *
 * Why plain methods instead of a StateFlow (unlike IssueModeRepository):
 * nothing needs to react to this value changing. The decision is read
 * ONCE per launch, before the splash starts (see StartupSplashViewModel),
 * and must not flip mid-animation when the flag is written at the end.
 *
 * The in-app "Play App Start Animation" replay never touches this: it
 * always plays the full animation and doesn't record anything.
 */
class StartupSplashRepository(private val storage: StartupSplashStorage) {

    val hasSeenFullAnimation: Boolean
        get() = storage.hasSeenFullAnimation()

    fun markFullAnimationSeen() {
        storage.markFullAnimationSeen()
    }

    companion object {
        @Volatile
        private var _instance: StartupSplashRepository? = null

        /** The app-wide instance — same plain-singleton-with-init(context) pattern as IssueModeRepository (this project has no DI framework yet). */
        val instance: StartupSplashRepository
            get() = checkNotNull(_instance) {
                "StartupSplashRepository.init(context) must be called before use " +
                    "(MainActivity.onCreate does this before setContent)."
            }

        /** Safe to call more than once: only the first call constructs the instance. */
        fun init(context: Context) {
            if (_instance != null) return
            _instance = StartupSplashRepository(SharedPreferencesStartupSplashStorage(context))
        }
    }
}
