package io.github.genesjc.codefixpanda.ui.welcome

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.genesjc.codefixpanda.R

/**
 * One step of the token guide. [glyph] is a drawing, not wording. A step shows either the address it sends
 * the user to ([showsLink]) or a screenshot of GitHub's page ([screenshot], described for screen readers by
 * [screenshotDescription]). [emphasis] is an optional highlighted reminder above the picture.
 * [showGetToken] adds a button that opens GitHub's token page, on the steps where the user still needs to go there.
 */
internal data class TokenGuideStep(
    val glyph: String,
    @StringRes val title: Int,
    @StringRes val body: Int,
    val showsLink: Boolean,
    @DrawableRes val screenshot: Int?,
    @StringRes val screenshotDescription: Int?,
    @StringRes val emphasis: Int?,
    val showGetToken: Boolean,
)

/** The token guide, in order: the page we send you to, name the token, leave every permission unticked, copy the token. */
internal val tokenGuideSteps = listOf(
    TokenGuideStep(
        glyph = "🔗",
        title = R.string.guide_step_1_title,
        body = R.string.guide_step_1_body,
        showsLink = true,
        screenshot = null,
        screenshotDescription = null,
        emphasis = null,
        showGetToken = true,
    ),
    TokenGuideStep(
        glyph = "✏️",
        title = R.string.guide_step_2_title,
        body = R.string.guide_step_2_body,
        showsLink = false,
        screenshot = R.drawable.token_guide_form,
        screenshotDescription = R.string.guide_step_2_screenshot,
        emphasis = null,
        showGetToken = true,
    ),
    TokenGuideStep(
        glyph = "☐",
        title = R.string.guide_step_3_title,
        body = R.string.guide_step_3_body,
        showsLink = false,
        screenshot = R.drawable.token_guide_submit,
        screenshotDescription = R.string.guide_step_3_screenshot,
        emphasis = R.string.guide_step_3_emphasis,
        showGetToken = false,
    ),
    TokenGuideStep(
        glyph = "📋",
        title = R.string.guide_step_4_title,
        body = R.string.guide_step_4_body,
        showsLink = false,
        screenshot = R.drawable.token_guide_copy,
        screenshotDescription = R.string.guide_step_4_screenshot,
        emphasis = null,
        showGetToken = false,
    ),
)
