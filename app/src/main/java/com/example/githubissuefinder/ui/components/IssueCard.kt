package com.example.githubissuefinder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.githubissuefinder.model.Issue

/**
 * The core feed card. `isBookmarked` is passed in rather than read from the
 * Issue model, since bookmark state now lives in shared state at the
 * navigation level (see AppNavHost.kt) — this keeps the sample data
 * immutable and the bookmark state as the single source of truth.
 */
@Composable
fun IssueCard(
    issue: Issue,
    isBookmarked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isBookmarked) 4.dp else 1.dp,
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                )
                Text(
                    text = issue.repoFullName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
                if (isBookmarked) {
                    Text(
                        text = "★ saved",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

            Text(
                text = issue.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                modifier = Modifier.padding(top = 8.dp, bottom = 10.dp),
            )

            if (issue.labels.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 10.dp),
                ) {
                    issue.labels.take(3).forEach { label -> LabelChip(label) }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (issue.language != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                        Text(
                            text = issue.language,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                } else {
                    Box(modifier = Modifier)
                }

                Text(
                    text = "${issue.commentCount} comments · ${issue.updatedAtLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
