package com.example.githubissuefinder.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
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
 *
 * `wrap` (added for TopReposScreen's landscape split view): false (the
 * default) keeps the original single-line, horizontally-scrolling
 * LazyRow — fine when this row spans the full screen width. true
 * switches to a FlowRow instead, wrapping chips onto as many lines as
 * needed rather than scrolling — needed once this row moved into a
 * narrow landscape side column, where a horizontal scroll would mean
 * two competing scroll directions on screen at once (this row
 * scrolling sideways, the repo list scrolling down) and would hide
 * chips off-screen with no visual hint they're there. Wrapping instead
 * keeps every language option visible at once, at the cost of the
 * column using more vertical space for this row — an accepted
 * tradeoff in a column that isn't showing anything else below it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LanguageFilterRow(
    selected: LanguageFilter,
    onSelect: (LanguageFilter) -> Unit,
    modifier: Modifier = Modifier,
    wrap: Boolean = false,
) {
    if (wrap) {
        FlowRow(
            modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LanguageFilter.entries.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = { Text(option.displayName) },
                )
            }
        }
    } else {
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
}
