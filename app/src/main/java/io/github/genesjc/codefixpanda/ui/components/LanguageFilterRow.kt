package io.github.genesjc.codefixpanda.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.genesjc.codefixpanda.model.LanguageFilter
import io.github.genesjc.codefixpanda.ui.text.nameRes

/**
 * The language filter chip row. `wrap` false (the default) is a single horizontally scrolling line;
 * true wraps the chips onto as many lines as needed, used in the landscape side rail where a sideways
 * scroll would compete with the list's vertical scroll and hide chips off-screen.
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
                    label = { Text(stringResource(option.nameRes())) },
                    colors = languageChipColors(),
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
                    label = { Text(stringResource(option.nameRes())) },
                    colors = languageChipColors(),
                )
            }
        }
    }
}

/** The chosen language is the seafoam: "what you have selected" has its own color, apart from the mode's. */
@Composable
private fun languageChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.tertiary,
    selectedLabelColor = MaterialTheme.colorScheme.onTertiary,
)
