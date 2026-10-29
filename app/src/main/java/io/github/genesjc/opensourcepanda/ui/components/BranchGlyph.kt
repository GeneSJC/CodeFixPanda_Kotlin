package io.github.genesjc.opensourcepanda.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
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

/** The outline on each side, in design units. */
private const val OUTLINE_UNITS = 1.0f

private val GLYPH_CENTER = Offset(60.65f, 54f)

/** What the glyph spans in design units, without its outline. */
internal const val BRANCH_GLYPH_WIDTH = 55.6f
internal const val BRANCH_GLYPH_HEIGHT = 66.8f

/**
 * Draws the black-outlined sun-yellow branch glyph centered in the draw area, [unit] pixels per design
 * unit. The outline is drawn first as the same shapes widened, so one pass of the fill covers the middle.
 */
internal fun DrawScope.drawBranchGlyph(unit: Float) {
    drawGlyphPass(unit, BrandOutlineBlack, extra = OUTLINE_UNITS * 2)
    drawGlyphPass(unit, BrandSun, extra = 0f)
}

/** The pixels per design unit that make the outlined glyph exactly [heightPx] tall. */
internal fun branchGlyphUnitForHeight(heightPx: Float): Float = heightPx / (BRANCH_GLYPH_HEIGHT + OUTLINE_UNITS * 2)

/** The glyph at a given height, for a place where it is simply an image (a title bar). */
@Composable
internal fun BranchGlyph(height: Dp, modifier: Modifier = Modifier) {
    val outlinedWidth = BRANCH_GLYPH_WIDTH + OUTLINE_UNITS * 2
    val outlinedHeight = BRANCH_GLYPH_HEIGHT + OUTLINE_UNITS * 2
    Canvas(modifier = modifier.height(height).aspectRatio(outlinedWidth / outlinedHeight)) {
        drawBranchGlyph(branchGlyphUnitForHeight(size.height))
    }
}

/** One pass of the glyph in [color]; [extra] widens every stroke and node, which is how the outline is drawn. */
private fun DrawScope.drawGlyphPass(unit: Float, color: Color, extra: Float) {
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
