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
import androidx.compose.material3.Surface
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
 *
 * v0.39 ("CM B"): the repo identity row is now two-tier — repoDisplayTitle
 * (a humanized, readable name) as the more prominent line, with
 * repoFullName (the raw owner/name path) demoted to a smaller line
 * underneath. Addresses the original complaint directly: a mixed,
 * cross-repo list (Feed) previously gave the repo only a small gray
 * path, no clearly readable identity.
 *
 * showExternalLinkIndicator (v0.38) is false by default, matching
 * Feed's behavior — tapping a Feed card navigates to the in-app
 * DetailScreen, not a browser, so no indicator is shown there.
 * RepoIssuesScreen passes true, since tapping a card there opens the
 * issue directly in GitHub. v0.39 enlarged this to 2.5x its original
 * text scale in a rounded badge; v0.40 reduced that to ~1.67x (2.5x
 * cut by one third) and halved the badge's vertical padding, per
 * direct feedback that 2.5x read as larger than intended.
 */
@Composable
fun IssueCard(
    issue: Issue,
    isBookmarked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showExternalLinkIndicator: Boolean = false,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isBookmarked) 4.dp else 1.dp,
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Box {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                    )
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            text = issue.repoDisplayTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = issue.repoFullName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
                    modifier = Modifier.padding(
                        top = 8.dp,
                        bottom = 10.dp,
                        end = if (showExternalLinkIndicator) 56.dp else 0.dp,
                    ),
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

            if (showExternalLinkIndicator) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 14.dp),
                ) {
                    Text(
                        "↗",
                        style = MaterialTheme.typography.titleMedium.copy(
                            // Reduced from 2.5x by one third per feedback: 2.5 * (2/3) ≈ 1.67x.
                            fontSize = MaterialTheme.typography.titleMedium.fontSize * 1.67f,
                        ),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp),
                    )
                }
            }
        }
    }
}
