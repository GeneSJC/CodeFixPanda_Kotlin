package io.github.genesjc.opensourcepanda.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import io.github.genesjc.opensourcepanda.R

/**
 * How much bigger every piece of splash text is than the Material
 * defaults it started from (70% larger), so the app name fills more of
 * the screen. Every size derived from the text (line heights, letter
 * spacing, gaps, the impact burst) is scaled by the same factor so the
 * wordmark keeps its original proportions.
 */
internal const val SPLASH_TEXT_SCALE = 1.7f

private const val PANDA_BASE_FONT_SP = 20f
private const val PANDA_BASE_LETTER_SPACING_SP = 4f

/** "Open Source" is a long headline, so it is set smaller to fit a phone's width. */
private const val HEADLINE_FIT_FACTOR = 0.62f

/**
 * The panda's height. The artwork is the launcher icon's panda, cropped, and is only about 300 px tall, so
 * this stays modest: larger would look soft on a high-density screen.
 */
private val MASCOT_HEIGHT = 130.dp

/** Scales a text unit, leaving an unspecified one alone (multiplying one would throw). */
private fun TextUnit.scaledBy(factor: Float): TextUnit = if (isSpecified) this * factor else this

/**
 * The panda above the app name laid out as "Open|Source" over "PANDA", centered —
 * shared by the full title-reveal animation (which passes motion
 * modifiers for each piece) and the short splash (which passes only
 * a fade), so the short version starts on the animation's final frame
 * and the two can never drift apart in size or layout. [mascotBackdrop]
 * is drawn behind the panda, centered on it (the splash's burst of light).
 */
@Composable
internal fun TitleWordmark(
    modifier: Modifier = Modifier,
    openModifier: Modifier = Modifier,
    sourceModifier: Modifier = Modifier,
    pandaModifier: Modifier = Modifier,
    mascotModifier: Modifier = Modifier,
    mascotBackdrop: @Composable BoxScope.() -> Unit = {},
) {
    // Shared by both "Open" and "Source": a soft colored glow beneath the
    // wordmark, matching the reference's own drop-shadowed look — plain
    // Bold alone read flatter on-device than the reference's heavier
    // weight + shadow combination.
    val display = MaterialTheme.typography.displayMedium
    val wordmarkStyle = display.copy(
        fontSize = display.fontSize.scaledBy(SPLASH_TEXT_SCALE * HEADLINE_FIT_FACTOR),
        lineHeight = display.lineHeight.scaledBy(SPLASH_TEXT_SCALE * HEADLINE_FIT_FACTOR),
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.35f),
            offset = Offset(0f, 8f),
            blurRadius = 28f,
        ),
    )
    val title = MaterialTheme.typography.titleMedium
    val pandaStyle = title.copy(
        fontSize = (PANDA_BASE_FONT_SP * SPLASH_TEXT_SCALE).sp,
        lineHeight = title.lineHeight.scaledBy(SPLASH_TEXT_SCALE),
        letterSpacing = (PANDA_BASE_LETTER_SPACING_SP * SPLASH_TEXT_SCALE).sp,
    )

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            mascotBackdrop()
            // Decorative: the name below already says what this is.
            Image(
                painter = painterResource(R.drawable.panda_mark),
                contentDescription = null,
                modifier = Modifier.height(MASCOT_HEIGHT).then(mascotModifier),
            )
        }
        Spacer(modifier = Modifier.height((8 * SPLASH_TEXT_SCALE).dp))
        Row {
            Text(
                "Open",
                style = wordmarkStyle,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = openModifier,
            )
            Spacer(modifier = Modifier.width((8 * SPLASH_TEXT_SCALE * HEADLINE_FIT_FACTOR).dp))
            Text(
                "Source",
                style = wordmarkStyle,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.secondary,
                modifier = sourceModifier,
            )
        }
        Spacer(modifier = Modifier.height((8 * SPLASH_TEXT_SCALE).dp))
        Text(
            "PANDA",
            style = pandaStyle,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = pandaModifier,
        )
    }
}
