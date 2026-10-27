package io.github.genesjc.githubissuefinder.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import io.github.genesjc.githubissuefinder.R

/**
 * The one-line "not affiliated with GitHub" notice, shown on every screen that names GitHub (the
 * welcome, token guide, mode gate and Settings screens, and the menu header) so the app is never read
 * as made or endorsed by GitHub. [color] is for the blue front-door screens, which are not themed.
 */
@Composable
fun IndependentNotice(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    textAlign: TextAlign = TextAlign.Center,
) {
    Text(
        stringResource(R.string.independent_notice),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        textAlign = textAlign,
        modifier = modifier,
    )
}
