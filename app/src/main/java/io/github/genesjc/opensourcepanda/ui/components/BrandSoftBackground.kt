package io.github.genesjc.opensourcepanda.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import io.github.genesjc.opensourcepanda.ui.theme.TangerineGlyph
import io.github.genesjc.opensourcepanda.ui.theme.TangerineLight
import io.github.genesjc.opensourcepanda.ui.theme.TangerineMid

/** How tall the glyph is relative to the area, and how far it is moved right and down from the middle (as fractions of it). */
private const val GLYPH_HEIGHT_FRACTION = 0.6f
private const val GLYPH_SHIFT_X = 0.28f
private const val GLYPH_SHIFT_Y = 0.12f

/** The glyph is faint, so the content on top stays the first thing seen. */
private const val GLYPH_ALPHA = 0.4f

/**
 * The backdrop for the content under the brand title bar: a pale tangerine gradient with the branch glyph
 * in a deeper tangerine, large, faint and bleeding off the right edge, behind [content].
 */
@Composable
internal fun BrandSoftBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(TangerineLight, TangerineMid))),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = GLYPH_ALPHA
                    compositingStrategy = CompositingStrategy.Offscreen
                },
        ) {
            val unit = size.height * GLYPH_HEIGHT_FRACTION / BRANCH_GLYPH_HEIGHT
            translate(left = size.width * GLYPH_SHIFT_X, top = size.height * GLYPH_SHIFT_Y) {
                drawBranchGlyph(unit, fill = TangerineGlyph, outlined = false)
            }
        }
        content()
    }
}
