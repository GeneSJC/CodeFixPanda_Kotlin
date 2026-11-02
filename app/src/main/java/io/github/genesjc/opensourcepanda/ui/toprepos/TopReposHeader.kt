package io.github.genesjc.opensourcepanda.ui.toprepos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.genesjc.opensourcepanda.R
import io.github.genesjc.opensourcepanda.model.IssueLabelFilter
import io.github.genesjc.opensourcepanda.ui.text.nameRes
import io.github.genesjc.opensourcepanda.ui.text.relativeTimeText
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
 * The header's title: a see-through white badge holding the mode's name in title-sized text and, under it, the
 * repo count (a tappable chip that opens the ranking explanation) and the updated line. The badge is as wide
 * as the space it is given and only as tall as its content, so a long name wraps and the badge grows.
 */
@Composable
internal fun ModeTitle(
    mode: IssueLabelFilter,
    repoCount: Int,
    lastUpdatedAtMs: Long?,
    onRepoCountClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = MaterialTheme.typography.labelLarge.let {
        it.copy(fontSize = it.fontSize * TITLE_TEXT_SCALE, lineHeight = it.lineHeight * TITLE_TEXT_SCALE)
    }
    Column(
        modifier = modifier
            .background(BrandOnOrange.copy(alpha = TITLE_PILL_ALPHA), RoundedCornerShape(8.dp))
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 10.dp),
    ) {
        Text(stringResource(mode.nameRes()), style = style, color = BrandOnOrange)
        Row(
            modifier = Modifier.padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RepoCountChip(repoCount, onClick = onRepoCountClick)
            UpdatedText(lastUpdatedAtMs, onBar = true)
        }
    }
}

/** The title is 30% larger than a label, since it is the screen's title. */
private const val TITLE_TEXT_SCALE = 1.3f

/** How much of the header's color the pill lets through, so it reads as a pill without a color of its own. */
private const val TITLE_PILL_ALPHA = 0.2f

@Composable
private fun repoCountLabel(count: Int): String = pluralStringResource(R.plurals.repo_count, count, count)
