package io.github.genesjc.opensourcepanda.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.genesjc.opensourcepanda.model.IssueLabelFilter
import io.github.genesjc.opensourcepanda.ui.nameRes
import io.github.genesjc.opensourcepanda.ui.theme.colorScheme

/** The active mode's name on its own color, with an optional smaller [subtitle] under it (the language, on a repo's screen). */
@Composable
fun ModePill(
    mode: IssueLabelFilter,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val scheme = mode.colorScheme()
    Box(
        modifier = modifier
            .background(scheme.primary, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Column {
            Text(
                stringResource(mode.nameRes()),
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onPrimary,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onPrimary.copy(alpha = SUBTITLE_ALPHA),
                )
            }
        }
    }
}

/** The subtitle is a little softer than the name. */
private const val SUBTITLE_ALPHA = 0.8f
