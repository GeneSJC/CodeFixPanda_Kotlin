package io.github.genesjc.opensourcepanda.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.genesjc.opensourcepanda.model.IssueLabelFilter
import io.github.genesjc.opensourcepanda.ui.nameRes
import io.github.genesjc.opensourcepanda.ui.theme.BrandOnOrange
import io.github.genesjc.opensourcepanda.ui.theme.colorScheme

/**
 * The active mode's name on its own color. With [onInfoClick], the beginner-friendly mode also gets an
 * info icon, so a newcomer can see what the mode means. [onBar] is for a place on a mode-colored top bar,
 * where the mode's own color would vanish into the bar: the pill is a see-through white with larger white
 * text, and it works as the screen's title (give it a fixed width and the name wraps).
 */
@Composable
fun ModePill(
    mode: IssueLabelFilter,
    modifier: Modifier = Modifier,
    onBar: Boolean = false,
    onInfoClick: (() -> Unit)? = null,
) {
    val scheme = mode.colorScheme()
    val pillColor = if (onBar) BrandOnOrange.copy(alpha = ON_BAR_PILL_ALPHA) else scheme.primary
    val contentColor = if (onBar) BrandOnOrange else scheme.onPrimary
    Box(
        modifier = modifier
            .background(pillColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = if (onBar) ON_BAR_PILL_VERTICAL_PADDING else 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(mode.nameRes()),
                style = if (onBar) onBarTextStyle() else MaterialTheme.typography.labelLarge,
                color = contentColor,
                // In a pill of a fixed width the name wraps, and the icon keeps its place at the end.
                modifier = Modifier.weight(1f, fill = false),
            )
            if (onInfoClick != null && mode == IssueLabelFilter.GOOD_FIRST_ISSUE) {
                Text(
                    "ⓘ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .clickable(onClick = onInfoClick),
                )
            }
        }
    }
}

/** How much of the bar's color the pill lets through, so it reads as a pill without a color of its own. */
private const val ON_BAR_PILL_ALPHA = 0.2f

/** The pill's text on a bar, larger than the usual label because the pill is the screen's title there. */
@Composable
private fun onBarTextStyle() = MaterialTheme.typography.labelLarge.let {
    it.copy(fontSize = it.fontSize * ON_BAR_TEXT_SCALE, lineHeight = it.lineHeight * ON_BAR_TEXT_SCALE)
}

private const val ON_BAR_TEXT_SCALE = 1.2f
private val ON_BAR_PILL_VERTICAL_PADDING = 10.dp
