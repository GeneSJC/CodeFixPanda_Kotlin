package com.example.githubissuefinder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.githubissuefinder.model.IssueLabelFilter
import com.example.githubissuefinder.ui.theme.colorScheme

/**
 * The active mode's name on its own color. With [onInfoClick], "good first issue" also gets an
 * info icon, since it reads less clearly than "help wanted".
 */
@Composable
fun ModePill(
    mode: IssueLabelFilter,
    modifier: Modifier = Modifier,
    onInfoClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .background(mode.colorScheme().primary, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                mode.displayName,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
            )
            if (onInfoClick != null && mode == IssueLabelFilter.GOOD_FIRST_ISSUE) {
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
