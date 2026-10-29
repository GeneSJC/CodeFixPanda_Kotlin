package io.github.genesjc.opensourcepanda.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
 * info icon, so a newcomer can see what the mode means. [onBar] is for a place on the brand orange, where
 * the mode's own color could vanish (the help-wanted orange is the bar's orange): the pill turns white,
 * with a dot and the name in the mode's color.
 */
@Composable
fun ModePill(
    mode: IssueLabelFilter,
    modifier: Modifier = Modifier,
    onBar: Boolean = false,
    onInfoClick: (() -> Unit)? = null,
) {
    val scheme = mode.colorScheme()
    val pillColor = if (onBar) BrandOnOrange else scheme.primary
    val contentColor = if (onBar) scheme.primary else scheme.onPrimary
    Box(
        modifier = modifier
            .background(pillColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBar) {
                Box(Modifier.size(8.dp).background(scheme.primary, CircleShape))
                Spacer(Modifier.width(6.dp))
            }
            Text(
                stringResource(mode.nameRes()),
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
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
