package io.github.genesjc.githubissuefinder.ads

import org.koin.dsl.module

/**
 * The open (foss) build's ads: none. It binds the do-nothing controller, so the shared code asks for an
 * [AdsController] and never knows which build it is in. The commercial flavor has a file with the same name
 * and package; while ads are parked (docs/ads-restore.md) it binds the do-nothing controller too.
 */
val adsModule = module {
    single<AdsController> { NoAdsController() }
}
