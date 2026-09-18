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
import com.example.githubissuefinder.model.LanguageFilter

/**
 * Shared language filter chip row, used by both Feed (v0.8) and the
 * Discover "Top Repos" landing screen (v0.26) — extracted here so both
 * screens render the identical set of language options from one source,
 * rather than duplicating the row. Same reasoning as extracting
 * SplashBrandMark so both splash animation variants share one resting
 * mark.
 */
@Composable
fun LanguageFilterRow(
    selected: LanguageFilter,
    onSelect: (LanguageFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
    ) {
        items(LanguageFilter.entries) { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(option.displayName) },
            )
        }
    }
}
