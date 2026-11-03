package io.github.genesjc.opensourcepanda.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import io.github.genesjc.opensourcepanda.ui.theme.LocalModeDecor

/** How far down from the middle the glyph is moved, as a fraction of the area's height. */
private const val GLYPH_SHIFT_Y = 0.12f

/**
 * The soft backdrop's glyph: a little over half the area's height, centered across it (so the nodes on both
 * sides show, with at most their edges touching the screen's), and fading in from 15% of the way along the
 * diagonal, so it is faint under the headline and strongest toward the bottom right.
 */
private const val SOFT_GLYPH_HEIGHT_FRACTION = 0.55f
private const val SOFT_GLYPH_SHIFT_X = 0.0f
private const val SOFT_FADE_START = 0.15f
private const val SOFT_GLYPH_MAX_ALPHA = 0.6f

/**
 * The backdrop for the content under the top bar, in the Issue Mode's colors (tangerine, or blue for
 * beginner-friendly): a pale gradient with the branch glyph in a deeper shade of it, large, centered and
 * fading in toward the bottom right, behind [content].
 */
@Composable
internal fun BrandSoftBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val decor = LocalModeDecor.current

    GlyphBackdrop(
        gradient = listOf(decor.backdropTop, decor.backdropBottom),
        glyphFill = decor.backdropGlyph,
        outlined = false,
        glyphMaxAlpha = SOFT_GLYPH_MAX_ALPHA,
        glyphHeightFraction = SOFT_GLYPH_HEIGHT_FRACTION,
        glyphShiftX = SOFT_GLYPH_SHIFT_X,
        fadeStart = SOFT_FADE_START,
        modifier = modifier,
        content = content,
    )
}

/**
 * A vertical [gradient] with the branch glyph on it (in [glyphFill], with the black outline when
 * [outlined]), [glyphHeightFraction] of the area tall and moved [glyphShiftX] of the width right of the middle,
 * fading in toward the bottom right from [fadeStart] of the way along the diagonal up to [glyphMaxAlpha], behind
 * [content]. The one drawing behind the soft backdrop and the front screens' orange.
 */
@Composable
internal fun GlyphBackdrop(
    gradient: List<Color>,
    glyphFill: Color,
    outlined: Boolean,
    glyphMaxAlpha: Float,
    glyphHeightFraction: Float,
    glyphShiftX: Float,
    fadeStart: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(gradient)),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
        ) {
            val unit = size.height * glyphHeightFraction / BRANCH_GLYPH_HEIGHT

            translate(left = size.width * glyphShiftX, top = size.height * GLYPH_SHIFT_Y) {
                drawBranchGlyph(unit, fill = glyphFill, outlined = outlined)
            }

            // Keeps only as much of the glyph as the fade lets through (only the mask's alpha counts).
            drawRect(
                brush = Brush.linearGradient(
                    0f to Color.Transparent,
                    fadeStart to Color.Transparent,
                    1f to glyphFill.copy(alpha = glyphMaxAlpha),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height),
                ),
                blendMode = BlendMode.DstIn,
            )
        }

        content()
    }
}
