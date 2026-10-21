package io.github.genesjc.githubissuefinder.ads

import android.content.Context
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.transformLatest

private const val PREFS_NAME = "ad_free_prefs"
private const val KEY_UNTIL_MS = "ad_free_until_ms"

/** How long the banner stays away after the user watches the rewarded video. */
const val AD_FREE_DURATION_MS: Long = 60L * 60 * 1000

/**
 * Where the end of the ad-free period is kept, split out so [AdFreePeriod] can be tested with no Android
 * Context. The stored value is epoch milliseconds, or 0 when there is no ad-free period.
 */
interface AdFreeStorage {
    fun load(): Long
    fun save(untilMs: Long)
}

/**
 * Plain SharedPreferences in the app's private storage. The file name is listed in
 * res/xml/backup_rules.xml and data_extraction_rules.xml so a restored backup cannot hand out a period;
 * keep them in sync if it changes.
 */
class SharedPreferencesAdFreeStorage(context: Context) : AdFreeStorage {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun load(): Long = prefs.getLong(KEY_UNTIL_MS, 0L)

    override fun save(untilMs: Long) {
        prefs.edit().putLong(KEY_UNTIL_MS, untilMs).apply()
    }
}

/**
 * The time the user has earned without the banner. Starting a period saves its end, so closing the app
 * does not lose it, and a period cannot be started while one is running (watching again earns nothing).
 * The time comes from the device clock, so a user who sets the clock back could stretch a period; a saved
 * end more than one period ahead of now is cut back to one period, which caps that. [nowMs] is a
 * parameter so tests control the clock.
 */
class AdFreePeriod(
    private val storage: AdFreeStorage,
    private val nowMs: () -> Long = System::currentTimeMillis,
    private val durationMs: Long = AD_FREE_DURATION_MS,
) {

    private val _untilMs = MutableStateFlow(storage.load().coerceAtMost(nowMs() + durationMs))

    /** When the current period ends, in epoch milliseconds; at or before now (or 0) when there is none. */
    val untilMs: StateFlow<Long> = _untilMs.asStateFlow()

    /** True while a period is running. */
    fun isActive(): Boolean = nowMs() < _untilMs.value

    /** True while a period is running, then false once it ends. Emits on its own at the end time. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val active: Flow<Boolean> = _untilMs.transformLatest { until ->
        val remainingMs = until - nowMs()
        if (remainingMs > 0) {
            emit(true)
            delay(remainingMs)
        }
        emit(false)
    }

    /** Starts a period of [durationMs] from now. Returns false, changing nothing, if one is already running. */
    fun start(): Boolean {
        if (isActive()) return false
        val until = nowMs() + durationMs
        storage.save(until)
        _untilMs.value = until
        return true
    }
}
