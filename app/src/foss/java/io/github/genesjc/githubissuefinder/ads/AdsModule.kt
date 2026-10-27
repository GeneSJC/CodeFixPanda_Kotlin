package io.github.genesjc.githubissuefinder.ads

import org.koin.dsl.module

/**
 * The open (foss) build's ads: none. The commercial flavor has a file with the same name and package
 * that supplies the real controller (or the same do-nothing one while its ads switch is off), so the
 * shared code asks for an [AdsController] and never knows which build it is in.
 */
val adsModule = module {
    single<AdsController> { NoAdsController() }
}
