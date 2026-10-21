package io.github.genesjc.githubissuefinder.ads

import android.app.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.dsl.module

/**
 * The open (foss) build's ads: none. The commercial flavor has a file with the same name and package
 * that supplies the real controller, so the shared code asks for an [AdsController] and never knows
 * which build it is in.
 */
val adsModule = module {
    single<AdsController> { NoAdsController() }
}

/** An [AdsController] that never allows an ad and has nothing to start. */
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
