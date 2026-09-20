package com.example.githubissuefinder.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
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
 */
@Composable
fun LabelFilterRow(
    selected: IssueLabelFilter,
    onSelect: (IssueLabelFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
    ) {
        items(IssueLabelFilter.entries) { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(option.displayName) },
            )
        }
    }
}
