package io.github.genesjc.githubissuefinder.testutil

import android.app.Activity
import io.github.genesjc.githubissuefinder.ads.AdsController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** An [AdsController] a test can drive: set the flows, and nothing else happens. */
class FakeAdsController(canShowAds: Boolean = false) : AdsController {

    override val canShowAds: MutableStateFlow<Boolean> = MutableStateFlow(canShowAds)

    override val privacyOptionsRequired: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override fun start(activity: Activity) = Unit

    override fun showPrivacyOptions(activity: Activity) = Unit
}
