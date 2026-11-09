package io.github.genesjc.codefixpanda.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.genesjc.codefixpanda.R
import io.github.genesjc.codefixpanda.model.Issue
import io.github.genesjc.codefixpanda.ui.text.issueBodyPreview
import io.github.genesjc.codefixpanda.ui.text.issueMetaText
import kotlinx.coroutines.launch

/**
 * The issue card: the title, its meta line (number, when it was opened, by whom) and the labels always show, and
 * an expand chevron reveals the body's first lines and a panel with the comment count and updated time, the
 * save, share and hide buttons and a copy-link badge.
 * `isBookmarked` is passed in, not read from Issue, so Issue stays immutable and the
 * bookmark repository stays the single source of truth. Bookmark, hide and the
 * external-link badge are optional: each appears only when its callback is given.
 * Hide is a plain callback with no Snackbar here, because the card is about to leave the
 * list and would cancel its own scope; the screen owns the Undo Snackbar. The revealed row is
 * IssueCardActions. More in _meta/docs/design-notes.md, "Issue card".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IssueCard(
    issue: Issue,
    isBookmarked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showExternalLinkIndicator: Boolean = false,
    onToggleBookmark: (() -> Unit)? = null,
    onHide: (() -> Unit)? = null,
    snackbarHostState: SnackbarHostState? = null,
) {
    val coroutineScope = rememberCoroutineScope()
    val bookmarkedMessage = stringResource(R.string.bookmarked_message)
    val unbookmarkedMessage = stringResource(R.string.unbookmarked_message)
    var expanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(CARD_EXPAND_COLLAPSE_DURATION_MS),
        label = "issueCardChevronRotation",
    )

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isBookmarked) 4.dp else 1.dp,
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // The title and its meta line, and the badges at the end: the same place, closed or open.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = issue.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                    )
                    issueMetaText(issue)?.let { meta ->
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (showExternalLinkIndicator) {
                        CardBadge(
                            icon = R.drawable.ic_open_in_new,
                            contentDescription = stringResource(R.string.action_open_on_github),
                            onClick = onClick,
                        )
                    }

                    CardBadge(
                        icon = R.drawable.ic_chevron_down,
                        contentDescription = stringResource(if (expanded) R.string.action_collapse else R.string.action_expand),
                        onClick = { expanded = !expanded },
                        rotation = chevronRotation,
                    )
                }
            }

            if (issue.labels.isNotEmpty()) {
                // Wraps when the labels do not fit.
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    issue.labels.take(3).forEach { label -> LabelChip(label) }
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(CARD_EXPAND_COLLAPSE_DURATION_MS)) +
                    fadeIn(animationSpec = tween(CARD_EXPAND_COLLAPSE_DURATION_MS)),
                exit = shrinkVertically(animationSpec = tween(CARD_EXPAND_COLLAPSE_DURATION_MS)) +
                    fadeOut(animationSpec = tween(CARD_EXPAND_COLLAPSE_DURATION_MS)),
            ) {
                Column {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                    val preview = remember(issue.bodyText) { issueBodyPreview(issue.bodyText) }
                    if (preview.isNotEmpty()) {
                        Text(
                            text = preview,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }

                    IssueCardActions(
                        issue = issue,
                        isBookmarked = isBookmarked,
                        onHide = onHide,
                        onToggleBookmark = onToggleBookmark?.let { toggle ->
                            {
                                toggle()
                                val message = if (isBookmarked) unbookmarkedMessage else bookmarkedMessage
                                snackbarHostState?.let { hostState ->
                                    coroutineScope.launch { hostState.showSnackbar(message) }
                                }
                            }
                        },
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }
    }
}

private const val CARD_EXPAND_COLLAPSE_DURATION_MS = 300

/**
 * The shared icon badge used for every small action on the card: a rounded square holding a vector icon,
 * [highlighted] in the seafoam for a saved state. `rotation` lets the caller animate the chevron. It is
 * clickable and not Surface(onClick), so it stays the size it looks instead of growing to a 48 dp touch target.
 */
@Composable
internal fun CardBadge(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    rotation: Float = 0f,
    highlighted: Boolean = false,
) {
    val shape = RoundedCornerShape(10.dp)

    Surface(
        shape = shape,
        color = if (highlighted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (highlighted) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.size(CARD_BADGE_SIZE).clip(shape).clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(icon),
                contentDescription = contentDescription,
                modifier = Modifier.size(CARD_BADGE_ICON_SIZE).rotate(rotation),
            )
        }
    }
}

private val CARD_BADGE_SIZE = 36.dp
private val CARD_BADGE_ICON_SIZE = 20.dp
