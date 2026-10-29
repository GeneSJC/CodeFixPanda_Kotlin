package io.github.genesjc.opensourcepanda.ui.welcome

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.genesjc.opensourcepanda.ui.components.GlyphBackdrop
import io.github.genesjc.opensourcepanda.ui.theme.BrandOrangeBottom
import io.github.genesjc.opensourcepanda.ui.theme.BrandOrangeMid
import io.github.genesjc.opensourcepanda.ui.theme.BrandOrangeTop
import io.github.genesjc.opensourcepanda.ui.theme.BrandSun

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
        modifier = modifier,
        content = content,
    )
}
