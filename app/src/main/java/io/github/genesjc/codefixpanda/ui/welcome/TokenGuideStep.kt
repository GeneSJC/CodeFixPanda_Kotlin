package io.github.genesjc.codefixpanda.ui.welcome

import androidx.annotation.StringRes
import io.github.genesjc.codefixpanda.R

/**
 * One step of the token guide. [glyph] is a drawing, not wording. [screenshotCaption] fills the screenshot
 * slot with text until a real screenshot of GitHub's page replaces it. [showGetToken] adds a button that
 * opens GitHub's token page, on the steps where the user still needs to go there.
 */
internal data class TokenGuideStep(
    val glyph: String,
    @StringRes val title: Int,
    @StringRes val body: Int,
    @StringRes val screenshotCaption: Int,
    val showGetToken: Boolean,
)

/** The token guide, in order: sign in, make the token, copy it, paste it into the app. */
internal val tokenGuideSteps = listOf(
    TokenGuideStep("🔑", R.string.guide_step_1_title, R.string.guide_step_1_body, R.string.guide_step_1_screenshot, showGetToken = true),
    TokenGuideStep("☐", R.string.guide_step_2_title, R.string.guide_step_2_body, R.string.guide_step_2_screenshot, showGetToken = true),
    TokenGuideStep("📋", R.string.guide_step_3_title, R.string.guide_step_3_body, R.string.guide_step_3_screenshot, showGetToken = false),
    TokenGuideStep("✅", R.string.guide_step_4_title, R.string.guide_step_4_body, R.string.guide_step_4_screenshot, showGetToken = false),
)
