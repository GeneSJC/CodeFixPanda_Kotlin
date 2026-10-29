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

/** How tall the glyph is relative to the area, and how far it is moved right and down from the middle (as fractions of it). */
private const val GLYPH_HEIGHT_FRACTION = 0.6f
private const val GLYPH_SHIFT_X = 0.28f
private const val GLYPH_SHIFT_Y = 0.12f

/**
 * The glyph fades in along the diagonal from the top left to the bottom right: invisible until
 * [FADE_START] of the way across, then up to the given maximum in the corner, so it never runs under the
 * headline and the text at the top and is strongest where the screen is empty.
 */
private const val FADE_START = 0.3f

/**
 * The backdrop for the content under the top bar, in the Issue Mode's colors (tangerine, or blue for
 * beginner-friendly): a pale gradient with the branch glyph in a deeper shade of it, large, bleeding off the
 * right edge and fading in toward the bottom right, behind [content]. [glyphShiftX] is how far (as a fraction
 * of the width) the glyph is moved right of the middle, and [fadeStart] how far across the diagonal the fade begins,
 * for a screen that wants more of the glyph showing.
 */
@Composable
internal fun BrandSoftBackground(
    modifier: Modifier = Modifier,
    glyphShiftX: Float = GLYPH_SHIFT_X,
    fadeStart: Float = FADE_START,
    content: @Composable BoxScope.() -> Unit,
) {
    val decor = LocalModeDecor.current
    GlyphBackdrop(
        gradient = listOf(decor.backdropTop, decor.backdropBottom),
        glyphFill = decor.backdropGlyph,
        outlined = false,
        glyphMaxAlpha = SOFT_GLYPH_MAX_ALPHA,
        glyphShiftX = glyphShiftX,
        fadeStart = fadeStart,
        modifier = modifier,
        content = content,
    )
}

private const val SOFT_GLYPH_MAX_ALPHA = 0.6f

/**
 * A vertical [gradient] with the branch glyph on it (in [glyphFill], with the black outline when
 * [outlined]), large, bleeding off the right edge and fading in toward the bottom right up to [glyphMaxAlpha],
 * behind [content]. The one drawing behind the soft backdrop and the front screens' orange.
 */
@Composable
internal fun GlyphBackdrop(
    gradient: List<Color>,
    glyphFill: Color,
    outlined: Boolean,
    glyphMaxAlpha: Float,
    modifier: Modifier = Modifier,
    glyphShiftX: Float = GLYPH_SHIFT_X,
    fadeStart: Float = FADE_START,
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
            val unit = size.height * GLYPH_HEIGHT_FRACTION / BRANCH_GLYPH_HEIGHT
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
