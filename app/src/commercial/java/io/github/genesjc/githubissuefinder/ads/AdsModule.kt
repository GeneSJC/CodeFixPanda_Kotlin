package io.github.genesjc.githubissuefinder.ads

import io.github.genesjc.githubissuefinder.BuildConfig
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * The commercial build's ads. This is the one place the ads switch is read: with `adsEnabled` on, the real
 * AdMob controller is bound; with it off, the do-nothing controller is, so the ads code stays in the build
 * but nothing runs (no consent form, no ads software started, no banner, no offer). The open (foss) flavor
 * has a file with the same name and package that always binds the do-nothing one.
 */
val adsModule = module {
    if (BuildConfig.ADS_ENABLED) {
        single { AdFreePeriod(SharedPreferencesAdFreeStorage(androidContext())) }
        single<AdsController> { AdMobAdsController(androidContext(), get()) }
    } else {
        single<AdsController> { NoAdsController() }
    }
}
