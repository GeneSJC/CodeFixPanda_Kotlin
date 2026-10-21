package io.github.genesjc.githubissuefinder.ads

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

/**
 * What the shared code knows about advertising: whether an ad may be shown now, whether the user
 * must be offered a way to reopen their privacy choices, and the two calls that need an Activity.
 * There is one implementation per build flavor, supplied by that flavor's Koin module: the open
 * (foss) build has one that never allows ads, so no advertising software is compiled into it, and
 * the commercial build has the real one. Nothing outside the flavor folders knows which is in use.
 */
interface AdsController {

    /** True once the user's consent state allows requesting ads. Always false in the foss build. */
    val canShowAds: StateFlow<Boolean>

    /** True when the app must offer a way to reopen the consent choices (privacy rules in some regions). */
    val privacyOptionsRequired: StateFlow<Boolean>

    /**
     * True only when this build can serve live (real) ads, which needs a production build with real ids.
     * Shown in the build label so a live build is never mistaken for a test one. Always false in the foss build.
     */
    val adsAreLive: Boolean

    /** Runs the consent flow, then starts the ads software. Safe to call again: it starts once. */
    fun start(activity: Activity)

    /** Reopens the consent choices. Only meaningful while [privacyOptionsRequired] is true. */
    fun showPrivacyOptions(activity: Activity)
}
