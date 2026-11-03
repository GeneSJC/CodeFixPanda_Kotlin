package io.github.genesjc.opensourcepanda.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import io.github.genesjc.opensourcepanda.R

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
    // labelSmall made 50% larger so the notice is easy to read.
    val noticeStyle = MaterialTheme.typography.labelSmall.let {
        it.copy(fontSize = it.fontSize * 1.5f, lineHeight = it.lineHeight * 1.5f)
    }

    Text(
        stringResource(R.string.independent_notice),
        style = noticeStyle,
        color = color,
        textAlign = textAlign,
        modifier = modifier,
    )
}
