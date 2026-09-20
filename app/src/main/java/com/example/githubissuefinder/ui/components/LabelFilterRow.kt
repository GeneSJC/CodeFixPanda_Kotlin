package com.example.githubissuefinder.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.githubissuefinder.model.IssueLabelFilter

/**
 * Shared label filter chip row, used by both Feed (v0.3) and the
 * repo-scoped RepoIssuesScreen (v0.38) — extracted here so both screens
 * render the identical set of label options from one source, same
 * reasoning as LanguageFilterRow's extraction in v0.27.
 *
 * disabledOptions and onDisabledOptionTapped (v0.40) support graying
 * out an option that's known to have zero matching issues for the
 * current repo — used by RepoIssuesScreen, unused by Feed (both
 * default to empty/no-op, so Feed's existing calls needed no changes).
 *
 * Deliberately does NOT use FilterChip's own `enabled = false` for
 * this — Compose's disabled state swallows the tap entirely, which
 * would make it impossible to show a Snackbar explaining why the chip
 * doesn't respond. Instead, the chip stays technically clickable
 * (so the tap registers) but is styled to look muted, and its onClick
 * branches: a disabled option's tap goes to onDisabledOptionTapped
 * instead of onSelect.
 */
@Composable
fun LabelFilterRow(
    selected: IssueLabelFilter,
    onSelect: (IssueLabelFilter) -> Unit,
    modifier: Modifier = Modifier,
    disabledOptions: Set<IssueLabelFilter> = emptySet(),
    onDisabledOptionTapped: (IssueLabelFilter) -> Unit = {},
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
    ) {
        items(IssueLabelFilter.entries) { option ->
            val isDisabled = option in disabledOptions
            FilterChip(
                selected = option == selected,
                onClick = {
                    if (isDisabled) onDisabledOptionTapped(option) else onSelect(option)
                },
                label = { Text(option.displayName) },
                colors = if (isDisabled) {
                    FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                } else {
                    FilterChipDefaults.filterChipColors()
                },
            )
        }
    }
}
