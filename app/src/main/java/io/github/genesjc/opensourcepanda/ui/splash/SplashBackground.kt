package io.github.genesjc.opensourcepanda.ui.splash

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import io.github.genesjc.opensourcepanda.ui.components.BRANCH_GLYPH_HEIGHT
import io.github.genesjc.opensourcepanda.ui.components.BRANCH_GLYPH_WIDTH
import io.github.genesjc.opensourcepanda.ui.components.drawBranchGlyph
import io.github.genesjc.opensourcepanda.ui.theme.BrandOrangeBottom
import io.github.genesjc.opensourcepanda.ui.theme.BrandOrangeMid
import io.github.genesjc.opensourcepanda.ui.theme.BrandOrangeTop

/** How much of the glyph's height and width, relative to the screen, it may fill; a little over 1 lets it bleed off the sides. */
private const val GLYPH_HEIGHT_FRACTION = 0.8f
private const val GLYPH_WIDTH_FRACTION = 1.15f

/** The glyph is see-through so the title and the panda stay the first thing seen. */
private const val GLYPH_ALPHA = 0.67f

/**
 * The splash's backdrop: the brand orange gradient with the branch glyph, black-outlined and partly
 * transparent, large behind [content] (centered).
 */
@Composable
internal fun SplashBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BrandOrangeTop, BrandOrangeMid, BrandOrangeBottom))),
        contentAlignment = Alignment.Center,
    ) {
        // Offscreen, so the outline and the fill fade as one shape and do not show through each other.
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = GLYPH_ALPHA
                    compositingStrategy = CompositingStrategy.Offscreen
                },
        ) {
            val unit = minOf(
                size.width * GLYPH_WIDTH_FRACTION / BRANCH_GLYPH_WIDTH,
                size.height * GLYPH_HEIGHT_FRACTION / BRANCH_GLYPH_HEIGHT,
            )
            drawBranchGlyph(unit)
        }
        content()
    }
}
