package io.github.genesjc.opensourcepanda.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import io.github.genesjc.opensourcepanda.model.IssueLabelFilter

/**
 * The decoration a screen takes from the Issue Mode: the gradient of its top bar, the gradient and glyph
 * color of its backdrop, and the fill and outline of its chips (the opposite mode's tone, so a chip stands
 * out from the backdrop without a bright white). It is not part of the ColorScheme because Material has no roles for it;
 * [LocalModeDecor] carries it down the tree from [OpenSourcePandaTheme], so a screen asks for "the
 * header" and "the backdrop" and never names a color.
 */
@Immutable
data class ModeDecor(
    val headerTop: Color,
    val headerBottom: Color,
    val backdropTop: Color,
    val backdropBottom: Color,
    val backdropGlyph: Color,
    val chipFill: Color,
    val chipOutline: Color,
)

/** Projects asking for help: the brand orange header on the tangerine backdrop, also what the start screen shows. */
private val HelpWantedDecor = ModeDecor(
    headerTop = BrandOrangeTop,
    headerBottom = BrandOrangeMid,
    backdropTop = TangerineLight,
    backdropBottom = TangerineMid,
    backdropGlyph = TangerineGlyph,
    chipFill = BlueChipFill,
    chipOutline = BlueChipOutline,
)

/** Beginner-friendly issues: the blue header on the blue backdrop. */
private val BeginnerDecor = ModeDecor(
    headerTop = BrandBlueLight,
    headerBottom = BrandBlue,
    backdropTop = BlueBackdropLight,
    backdropBottom = BlueBackdropMid,
    backdropGlyph = BlueBackdropGlyph,
    chipFill = TangerineChipFill,
    chipOutline = TangerineChipOutline,
)

/** What a screen with no mode chosen uses. */
val DefaultModeDecor = HelpWantedDecor

/** A `when` with no `else`, like [colorScheme]: a new mode will not compile until it has a decoration. */
fun IssueLabelFilter.decor(): ModeDecor = when (this) {
    IssueLabelFilter.GOOD_FIRST_ISSUE -> BeginnerDecor
    IssueLabelFilter.HELP_WANTED -> HelpWantedDecor
}

val LocalModeDecor = staticCompositionLocalOf { DefaultModeDecor }
