package com.example.githubissuefinder.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.githubissuefinder.data.formatRelativeTime
import com.example.githubissuefinder.model.IssueLabelFilter
import com.example.githubissuefinder.ui.theme.colorScheme

/** The mode pill: the active mode's name on its own color, with an info icon for "good first issue". */
@Composable
internal fun ModePill(mode: IssueLabelFilter, onInfoClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(mode.colorScheme().primary, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                mode.displayName,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
            )
            // Only "good first issue" gets an info icon; it reads less clearly than "help wanted".
            if (mode == IssueLabelFilter.GOOD_FIRST_ISSUE) {
                Text(
                    "ⓘ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .clickable(onClick = onInfoClick),
                )
            }
        }
    }
}

/** The repo-count chip and the "Updated X ago" line, as two siblings for the caller's Row. */
@Composable
internal fun RepoCountAndUpdated(repoCount: Int, lastUpdatedAtMs: Long?) {
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
    Text(
        lastUpdatedAtMs?.let { "Updated ${formatRelativeTime(it)}" }.orEmpty(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp),
    )
}

private fun repoCountLabel(count: Int): String = if (count == 1) "1 repo" else "$count repos"
