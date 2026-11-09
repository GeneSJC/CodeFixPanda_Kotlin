package io.github.genesjc.codefixpanda.config

import org.junit.Assert.assertEquals
import org.junit.Test

class SplashAnimationTest {

    @Test
    fun everyAnimationIsFoundByItsBuildSettingName() {
        SplashAnimation.entries.forEach { animation ->
            assertEquals(animation, SplashAnimation.fromConfigName(animation.configName))
        }
    }

    @Test
    fun theBuildSettingNamesAreTheOnesBuildGradleUses() {
        assertEquals(SplashAnimation.TITLE_REVEAL, SplashAnimation.fromConfigName("titleReveal"))
        assertEquals(SplashAnimation.SHAPE_STACK, SplashAnimation.fromConfigName("shapeStack"))
    }

    @Test(expected = IllegalStateException::class)
    fun anUnknownNameFailsInsteadOfPlayingADefault() {
        SplashAnimation.fromConfigName("titlReveal")
    }
}
