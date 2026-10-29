package io.github.genesjc.opensourcepanda.ui.splash

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import io.github.genesjc.opensourcepanda.ui.theme.BrandOrangeBottom
import io.github.genesjc.opensourcepanda.ui.theme.BrandOrangeMid
import io.github.genesjc.opensourcepanda.ui.theme.BrandOrangeTop
import io.github.genesjc.opensourcepanda.ui.theme.BrandOutlineBlack
import io.github.genesjc.opensourcepanda.ui.theme.BrandSun

// The branch glyph, in the launcher icon's design units (a 108 x 108 canvas), so it is the same drawing as the icon's.
private val TRUNK = listOf(Offset(76.57f, 79.80f), Offset(40.43f, 28.20f))
private val BRANCH = listOf(
    Offset(51.62f, 44.17f), Offset(54.51f, 46.17f), Offset(57.20f, 47.97f), Offset(60.01f, 49.60f),
    Offset(62.61f, 50.65f), Offset(65.25f, 51.39f), Offset(67.92f, 51.86f), Offset(70.53f, 52.05f),
    Offset(73.03f, 51.89f), Offset(75.68f, 51.23f), Offset(78.18f, 50.34f), Offset(80.86f, 49.33f),
)
private val NODES = listOf(Offset(76.57f, 79.80f), Offset(40.43f, 28.20f), Offset(80.86f, 49.33f))
private const val LINE_WIDTH = 4.6f
private const val NODE_RADIUS = 7.6f

// What the glyph spans, outline included, so it can be centered and sized to the screen.
private val GLYPH_CENTER = Offset(60.65f, 54f)
private const val GLYPH_WIDTH = 55.6f
private const val GLYPH_HEIGHT = 66.8f

/** How much of the glyph's height and width, relative to the screen, it may fill; a little over 1 lets it bleed off the sides. */
private const val GLYPH_HEIGHT_FRACTION = 0.8f
private const val GLYPH_WIDTH_FRACTION = 1.15f

/** The glyph's outline, on each side, in design units. */
private const val OUTLINE_UNITS = 1.0f

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
            val unit = minOf(size.width * GLYPH_WIDTH_FRACTION / GLYPH_WIDTH, size.height * GLYPH_HEIGHT_FRACTION / GLYPH_HEIGHT)
            drawGlyph(unit, BrandOutlineBlack, extra = OUTLINE_UNITS * 2)
            drawGlyph(unit, BrandSun, extra = 0f)
        }
        content()
    }
}

/** One pass of the glyph in [color]; [extra] widens every stroke and node, which is how the outline is drawn. */
private fun DrawScope.drawGlyph(unit: Float, color: Color, extra: Float) {
    fun at(point: Offset) = center + (point - GLYPH_CENTER) * unit
    val stroke = Stroke(width = (LINE_WIDTH + extra) * unit, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawLine(color, at(TRUNK[0]), at(TRUNK[1]), strokeWidth = stroke.width, cap = StrokeCap.Round)
    val branch = Path().apply {
        moveTo(at(BRANCH.first()).x, at(BRANCH.first()).y)
        BRANCH.drop(1).forEach { lineTo(at(it).x, at(it).y) }
    }
    drawPath(branch, color, style = stroke)
    NODES.forEach { drawCircle(color, radius = (NODE_RADIUS + extra / 2) * unit, center = at(it)) }
}
