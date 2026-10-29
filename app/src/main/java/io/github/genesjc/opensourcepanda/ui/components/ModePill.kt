package io.github.genesjc.opensourcepanda.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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

/** The active mode's name on its own color. */
@Composable
fun ModePill(
    mode: IssueLabelFilter,
    modifier: Modifier = Modifier,
) {
    val scheme = mode.colorScheme()
    Box(
        modifier = modifier
            .background(scheme.primary, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            stringResource(mode.nameRes()),
            style = MaterialTheme.typography.labelLarge,
            color = scheme.onPrimary,
        )
    }
}
