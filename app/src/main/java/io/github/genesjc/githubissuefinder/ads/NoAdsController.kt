package io.github.genesjc.githubissuefinder.ads

import android.app.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * An [AdsController] that never allows an ad and has nothing to start: no consent form, no ads software
 * started, no banner, no offer. The open (foss) build always binds it. The commercial build binds it too
 * while the ads switch (`adsEnabled` in the Gradle file) is off, which is how the ads code stays in the
 * project but does nothing. It lives in shared code so both flavors can use it.
 */
class NoAdsController : AdsController {

    override val canShowAds: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()

    override val privacyOptionsRequired: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()

    override val adsAreLive: Boolean = false

    override val adFreeUntilMs: StateFlow<Long> = MutableStateFlow(0L).asStateFlow()

    override val canOfferAdFree: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()

    override fun start(activity: Activity) = Unit

    override fun showAdFreeOffer(activity: Activity) = Unit

    override fun showPrivacyOptions(activity: Activity) = Unit
}
