package io.github.genesjc.githubissuefinder.ads

import org.koin.dsl.module

/**
 * PARKED: ads are not part of this build, so the commercial flavor binds the do-nothing controller. The real
 * module (the AdMob controller and its ad-free period) is in the tag ads-parked-2026-10-27; docs/ads-restore.md
 * says how to bring it back. Restoring overwrites this file.
 */
val adsModule = module {
    single<AdsController> { NoAdsController() }
}
