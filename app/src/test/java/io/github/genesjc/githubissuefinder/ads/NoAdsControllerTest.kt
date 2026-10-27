package io.github.genesjc.githubissuefinder.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** The do-nothing controller (both flavors, while ads are parked) never allows an ad, never asks for privacy options and never offers an ad-free period. */
class NoAdsControllerTest {

    private val controller = NoAdsController()

    @Test
    fun neverAllowsAnAd() {
        assertFalse(controller.canShowAds.value)
    }

    @Test
    fun neverAsksForPrivacyOptions() {
        assertFalse(controller.privacyOptionsRequired.value)
    }

    @Test
    fun neverOffersAnAdFreePeriod() {
        assertFalse(controller.canOfferAdFree.value)
        assertEquals(0L, controller.adFreeUntilMs.value)
    }
}
