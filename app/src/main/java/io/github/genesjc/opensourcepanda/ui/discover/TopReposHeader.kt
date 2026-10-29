package io.github.genesjc.opensourcepanda.ui.discover

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.genesjc.opensourcepanda.R
import io.github.genesjc.opensourcepanda.ui.relativeTimeText
import io.github.genesjc.opensourcepanda.ui.theme.BrandSoftWhite

/** The repo-count chip. */
@Composable
internal fun RepoCountChip(repoCount: Int) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            repoCountLabel(repoCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
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
internal fun RepoCountAndUpdated(repoCount: Int, lastUpdatedAtMs: Long?) {
    RepoCountChip(repoCount)
    UpdatedText(lastUpdatedAtMs, modifier = Modifier.padding(start = 8.dp))
}

@Composable
private fun repoCountLabel(count: Int): String = pluralStringResource(R.plurals.repo_count, count, count)
