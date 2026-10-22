package io.github.genesjc.githubissuefinder.ui.welcome

import androidx.annotation.StringRes
import io.github.genesjc.githubissuefinder.R

/**
 * One page of the welcome slideshow. [glyph] is a drawing, not wording. [screenshotCaption] fills the
 * screenshot slot with text until a real screenshot replaces it.
 */
internal data class WelcomeSlide(
    val glyph: String,
    @StringRes val title: Int,
    @StringRes val body: Int,
    @StringRes val screenshotCaption: Int,
)

/** The slideshow, in order. The last slide carries the token entry, which the screen adds. */
internal val welcomeSlides = listOf(
    WelcomeSlide("🔎", R.string.welcome_slide_1_title, R.string.welcome_slide_1_body, R.string.welcome_slide_1_screenshot),
    WelcomeSlide("🎯", R.string.welcome_slide_2_title, R.string.welcome_slide_2_body, R.string.welcome_slide_2_screenshot),
    WelcomeSlide("🔑", R.string.welcome_slide_3_title, R.string.welcome_slide_3_body, R.string.welcome_slide_3_screenshot),
    WelcomeSlide("☐", R.string.welcome_slide_4_title, R.string.welcome_slide_4_body, R.string.welcome_slide_4_screenshot),
    WelcomeSlide("📋", R.string.welcome_slide_5_title, R.string.welcome_slide_5_body, R.string.welcome_slide_5_screenshot),
)
