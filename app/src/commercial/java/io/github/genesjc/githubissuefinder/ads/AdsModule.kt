package io.github.genesjc.githubissuefinder.ads

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * The commercial build's ads. The open (foss) flavor has a file with the same name and package that
 * supplies a controller that never shows ads.
 */
val adsModule = module {
    single { AdFreePeriod(SharedPreferencesAdFreeStorage(androidContext())) }
    single<AdsController> { AdMobAdsController(androidContext(), get()) }
}
