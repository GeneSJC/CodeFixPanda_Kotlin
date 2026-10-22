package io.github.genesjc.githubissuefinder.ui.ads

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Where a screen's ad banner goes: it slides and fades in and out as [visible] changes, so the list's
 * bottom edge eases instead of jumping when the ad-free period starts or ends. The banner itself is
 * the flavor's [AdBanner].
 */
@Composable
fun AdBannerSlot(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        AdBanner(modifier = modifier)
    }
}
