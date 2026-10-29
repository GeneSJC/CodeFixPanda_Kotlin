package io.github.genesjc.opensourcepanda.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.genesjc.opensourcepanda.R
import io.github.genesjc.opensourcepanda.model.IssueLabelFilter
import io.github.genesjc.opensourcepanda.ui.nameRes
import io.github.genesjc.opensourcepanda.ui.relativeTimeText
import io.github.genesjc.opensourcepanda.ui.theme.BrandOnOrange
import io.github.genesjc.opensourcepanda.ui.theme.BrandSoftWhite

/** The repo-count chip; with [onClick] it is tappable and opens the ranking explanation. */
@Composable
internal fun RepoCountChip(repoCount: Int, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val label: @Composable () -> Unit = {
        Text(
            repoCountLabel(repoCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
    val shape = RoundedCornerShape(12.dp)
    // Tappable through clickable and not Surface(onClick), which would pad the chip out to Material's 48 dp touch target.
    val tapModifier = if (onClick != null) modifier.clip(shape).clickable(onClick = onClick) else modifier
    Surface(shape = shape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = tapModifier, content = label)
}

/** The "Updated X ago" line. [onBar] is for a mode-colored header, where it is the soft white and not the usual dark gray. */
@Composable
internal fun UpdatedText(lastUpdatedAtMs: Long?, modifier: Modifier = Modifier, onBar: Boolean = false) {
    Text(
        lastUpdatedAtMs?.let { stringResource(R.string.repo_list_updated, relativeTimeText(it)) }.orEmpty(),
        style = MaterialTheme.typography.labelSmall,
        color = if (onBar) BrandSoftWhite else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/** The count chip and the updated line as two siblings for the caller's Row (the landscape side rail). */
@Composable
internal fun RepoCountAndUpdated(repoCount: Int, lastUpdatedAtMs: Long?, onCountClick: () -> Unit) {
    RepoCountChip(repoCount, onClick = onCountClick)
    UpdatedText(lastUpdatedAtMs, modifier = Modifier.padding(start = 8.dp))
}

/**
 * The header's title: the mode's name on a see-through white pill, as large as a title. The pill is as wide
 * as the space it is given and only as tall as the (wrapped) name with its padding.
 */
@Composable
internal fun ModeTitle(mode: IssueLabelFilter, modifier: Modifier = Modifier) {
    val style = MaterialTheme.typography.labelLarge.let {
        it.copy(fontSize = it.fontSize * TITLE_TEXT_SCALE, lineHeight = it.lineHeight * TITLE_TEXT_SCALE)
    }
    Box(
        modifier = modifier
            .background(BrandOnOrange.copy(alpha = TITLE_PILL_ALPHA), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(stringResource(mode.nameRes()), style = style, color = BrandOnOrange)
    }
}

/** The title is 30% larger than a label, since it is the screen's title. */
private const val TITLE_TEXT_SCALE = 1.3f

/** How much of the header's color the pill lets through, so it reads as a pill without a color of its own. */
private const val TITLE_PILL_ALPHA = 0.2f

@Composable
private fun repoCountLabel(count: Int): String = pluralStringResource(R.plurals.repo_count, count, count)
