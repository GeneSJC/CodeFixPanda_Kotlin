package io.github.genesjc.opensourcepanda.ui.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// The launcher icon's blues, so the front door looks like the app's icon. The light end of the icon's
// gradient is left out because white text must stay readable over every part of the screen.
internal val GradientDark = Color(0xFF0B1736)
private val GradientMid = Color(0xFF1D4ED8)
private val GradientLight = Color(0xFF2563EB)

/** White at 80%, for secondary text and outlines on the gradient. */
internal val SoftWhite = Color(0xCCFFFFFF)

/** The icon's sky blue, for a border that shows against both the gradient and a white panel. */
internal val SkyBlue = Color(0xFF38BDF8)

/** The blue gradient behind the welcome screen and the token guide, running from the lower left to the upper right. */
@Composable
internal fun FrontDoorBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(GradientDark, GradientMid, GradientLight),
                    start = Offset(0f, Float.POSITIVE_INFINITY),
                    end = Offset(Float.POSITIVE_INFINITY, 0f),
                ),
            ),
        content = content,
    )
}
