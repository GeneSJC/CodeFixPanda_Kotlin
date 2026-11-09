package io.github.genesjc.codefixpanda.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.genesjc.codefixpanda.R
import io.github.genesjc.codefixpanda.ui.theme.BrandOrangeBottom
import io.github.genesjc.codefixpanda.ui.theme.BrandOrangeMid
import io.github.genesjc.codefixpanda.ui.theme.BrandOrangeTop

private val BAND_HEIGHT = 120.dp
private val GLYPH_HEIGHT = 96.dp
private val GAP = 10.dp

/**
 * The app's identity as a title bar: the orange gradient, the branch glyph, then the panda from the
 * head to the middle of the torso, standing on the band's bottom edge, as in the README banner. The
 * app's name is not written out; it is the band's description for screen readers. The band reaches
 * under the status bar, so [BAND_HEIGHT] is what shows below it.
 */
@Composable
internal fun BrandTitleBand(modifier: Modifier = Modifier) {
    val appName = stringResource(R.string.app_name)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(BrandOrangeTop, BrandOrangeMid, BrandOrangeBottom)))
            .statusBarsPadding()
            .height(BAND_HEIGHT)
            .semantics { contentDescription = appName },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom,
    ) {
        BranchGlyph(height = GLYPH_HEIGHT, modifier = Modifier.padding(bottom = (BAND_HEIGHT - GLYPH_HEIGHT) / 2))
        Image(
            painter = painterResource(R.drawable.panda_mark),
            contentDescription = null,
            modifier = Modifier.padding(start = GAP).height(BAND_HEIGHT),
        )
    }
}
