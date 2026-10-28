package io.github.genesjc.opensourcepanda.ads

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

/**
 * What the shared code knows about advertising: whether an ad may be shown now, whether the user
 * must be offered a way to reopen their privacy choices, and the two calls that need an Activity.
 * There is one implementation per build flavor, supplied by that flavor's Koin module. While ads are
 * parked (docs/ads-restore.md) both flavors bind the do-nothing [NoAdsController], so no advertising
 * software is compiled into either; the commercial flavor's real implementation is in the tag
 * ads-parked-2026-10-27. Nothing outside the flavor folders knows which is in use.
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

    /**
     * When the user's ad-free period ends, in epoch milliseconds; 0 or a past time when there is none. A period
     * is earned by watching one rewarded video, and while it runs [canShowAds] is false. Always 0 in the foss build.
     */
    val adFreeUntilMs: StateFlow<Long>

    /** True when the user can be offered the ad-free period now: ads allowed, no period running, a video ready. */
    val canOfferAdFree: StateFlow<Boolean>

    /** Runs the consent flow, then starts the ads software. Safe to call again: it starts once. */
    fun start(activity: Activity)

    /** Plays the rewarded video; finishing it starts the ad-free period. Only meaningful while [canOfferAdFree] is true. */
    fun showAdFreeOffer(activity: Activity)

    /** Reopens the consent choices. Only meaningful while [privacyOptionsRequired] is true. */
    fun showPrivacyOptions(activity: Activity)
}
