package io.github.genesjc.opensourcepanda.ui.welcome

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.genesjc.opensourcepanda.ui.components.GlyphBackdrop
import io.github.genesjc.opensourcepanda.ui.theme.BrandOrangeBottom
import io.github.genesjc.opensourcepanda.ui.theme.BrandOrangeMid
import io.github.genesjc.opensourcepanda.ui.theme.BrandOrangeTop
import io.github.genesjc.opensourcepanda.ui.theme.BrandSun

/** The glyph's size, its shift to the right of the middle and where its fade begins: larger and further right than on the soft backdrop, so it clears the centered text. */
private const val GLYPH_HEIGHT_FRACTION = 0.6f
private const val GLYPH_SHIFT_X = 0.28f
private const val FADE_START = 0.3f

/** How strong the glyph gets in the corner where it is strongest: less than on the soft backdrop, since white text sits on this one. */
private const val GLYPH_MAX_ALPHA = 0.5f

/** The brand orange gradient with the outlined branch glyph behind the welcome screen and the token guide. */
@Composable
internal fun FrontDoorBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    GlyphBackdrop(
        gradient = listOf(BrandOrangeTop, BrandOrangeMid, BrandOrangeBottom),
        glyphFill = BrandSun,
        outlined = true,
        glyphMaxAlpha = GLYPH_MAX_ALPHA,
        glyphHeightFraction = GLYPH_HEIGHT_FRACTION,
        glyphShiftX = GLYPH_SHIFT_X,
        fadeStart = FADE_START,
        modifier = modifier,
        content = content,
    )
}
