package io.github.genesjc.opensourcepanda.testutil

import android.app.Activity
import io.github.genesjc.opensourcepanda.ads.AdsController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** An [AdsController] a test can drive: set the flows, and nothing else happens. */
class FakeAdsController(canShowAds: Boolean = false) : AdsController {

    override val canShowAds: MutableStateFlow<Boolean> = MutableStateFlow(canShowAds)

    override val privacyOptionsRequired: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override val adsAreLive: Boolean = false

    override val adFreeUntilMs: MutableStateFlow<Long> = MutableStateFlow(0L)

    override val canOfferAdFree: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /** How many times a test has asked for the ad-free offer to be shown. */
    var adFreeOffersShown = 0
        private set

    override fun start(activity: Activity) = Unit

    override fun showAdFreeOffer(activity: Activity) {
        adFreeOffersShown++
    }

    override fun showPrivacyOptions(activity: Activity) = Unit
}
