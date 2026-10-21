package io.github.genesjc.githubissuefinder.ads

import org.junit.Assert.assertFalse
import org.junit.Test

/** The open build never allows an ad and never asks for privacy options. */
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
}
