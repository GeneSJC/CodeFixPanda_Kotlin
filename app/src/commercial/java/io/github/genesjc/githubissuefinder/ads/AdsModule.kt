package io.github.genesjc.githubissuefinder.ads

import android.app.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.dsl.module

/**
 * The commercial build's ads. The open (foss) flavor has a file with the same name and package that
 * supplies a controller that never shows ads.
 */
val adsModule = module {
    single<AdsController> { AdMobAdsController() }
}

/**
 * The commercial build's [AdsController]. It is a placeholder until the ads software is added: it
 * never allows an ad yet, so the build behaves like the open one.
 */
class AdMobAdsController : AdsController {

    override val canShowAds: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()

    override val privacyOptionsRequired: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()

    override fun start(activity: Activity) = Unit

    override fun showPrivacyOptions(activity: Activity) = Unit
}
