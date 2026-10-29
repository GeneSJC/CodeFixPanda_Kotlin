package io.github.genesjc.opensourcepanda.ui.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import io.github.genesjc.opensourcepanda.ui.theme.FrontDoorBlueDark
import io.github.genesjc.opensourcepanda.ui.theme.FrontDoorBlueLight
import io.github.genesjc.opensourcepanda.ui.theme.FrontDoorBlueMid

/** The blue gradient behind the welcome screen and the token guide, running from the lower left to the upper right. */
@Composable
internal fun FrontDoorBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(FrontDoorBlueDark, FrontDoorBlueMid, FrontDoorBlueLight),
                    start = Offset(0f, Float.POSITIVE_INFINITY),
                    end = Offset(Float.POSITIVE_INFINITY, 0f),
                ),
            ),
        content = content,
    )
}
