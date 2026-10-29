package io.github.genesjc.opensourcepanda.ui.splash

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp

/**
 * The soft radial pulse of the primary color that flashes behind the panda, centered on the box it is placed
 * in. [radius] is its radius at scale 1. It is drawn, not laid out: it takes the size of its box and does not
 * change it, so the pulse can grow far past the panda without pushing anything around. [burstScale] and
 * [burstAlpha] are read while drawing, so animating them does not recompose.
 */
@Composable
internal fun BoxScope.SplashBurst(radius: Dp, burstScale: () -> Float, burstAlpha: () -> Float) {
    val color = MaterialTheme.colorScheme.primary
    Spacer(
        modifier = Modifier.matchParentSize().drawBehind {
            val a = burstAlpha()
            val r = radius.toPx() * burstScale()
            if (a > 0f && r > 0f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to color.copy(alpha = 0.6f * a),
                        0.6f to color.copy(alpha = 0.18f * a),
                        1f to color.copy(alpha = 0f),
                        center = center,
                        radius = r,
                    ),
                    radius = r,
                    center = center,
                )
            }
        },
    )
}
