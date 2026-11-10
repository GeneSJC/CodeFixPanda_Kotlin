package io.github.genesjc.codefixpanda.ui.welcome

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.genesjc.codefixpanda.R

/**
 * One step of the token guide. [glyph] is a drawing, not wording. [screenshot] is a picture of GitHub's own
 * page, described for screen readers by [screenshotDescription]. [showsLink] adds the address of that page
 * above the picture. [emphasis] is an optional highlighted reminder above the picture. [screenshotWidthFraction]
 * narrows a tall picture so that it fits on screen without scrolling.
 */
internal data class TokenGuideStep(
    val glyph: String,
    @StringRes val title: Int,
    @StringRes val body: Int,
    @DrawableRes val screenshot: Int,
    @StringRes val screenshotDescription: Int,
    @StringRes val emphasis: Int?,
    val showsLink: Boolean,
    val screenshotWidthFraction: Float = 1f,
)

/** The token guide, in order: name the token, scroll down and save without ticking anything, copy the token, paste it into the app. */
internal val tokenGuideSteps = listOf(
    TokenGuideStep(
        glyph = "✏️",
        title = R.string.guide_step_1_title,
        body = R.string.guide_step_1_body,
        screenshot = R.drawable.token_guide_form,
        screenshotDescription = R.string.guide_step_1_screenshot,
        emphasis = null,
        showsLink = true,
    ),
    TokenGuideStep(
        glyph = "☐",
        title = R.string.guide_step_2_title,
        body = R.string.guide_step_2_body,
        screenshot = R.drawable.token_guide_submit,
        screenshotDescription = R.string.guide_step_2_screenshot,
        emphasis = R.string.guide_step_2_emphasis,
        showsLink = false,
    ),
    TokenGuideStep(
        glyph = "📋",
        title = R.string.guide_step_3_title,
        body = R.string.guide_step_3_body,
        screenshot = R.drawable.token_guide_copy,
        screenshotDescription = R.string.guide_step_3_screenshot,
        emphasis = null,
        showsLink = false,
    ),
    TokenGuideStep(
        glyph = "✅",
        title = R.string.guide_step_4_title,
        body = R.string.guide_step_4_body,
        screenshot = R.drawable.token_guide_paste,
        screenshotDescription = R.string.guide_step_4_screenshot,
        emphasis = null,
        showsLink = false,
        screenshotWidthFraction = 0.9f,
    ),
)
