package com.example.githubissuefinder.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.example.githubissuefinder.model.Issue
import kotlinx.coroutines.launch

/**
 * The issue card: the title and labels always show, and an expand chevron reveals an
 * action row (hide, bookmark, share, copy link) with the comment count and updated time.
 * `isBookmarked` is passed in, not read from Issue, so Issue stays immutable and the
 * bookmark repository stays the single source of truth. Bookmark, hide and the
 * external-link badge are optional: each appears only when its callback is given.
 * Hide is a plain callback with no Snackbar here, because the card is about to leave the
 * list and would cancel its own scope; the screen owns the Undo Snackbar. The revealed row is
 * IssueCardActions. More in docs/design-notes.md, "Issue card".
 */
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
        Box {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = issue.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth().padding(end = 56.dp),
                )

                if (issue.labels.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                    IssueCardActions(
                        issue = issue,
                        isBookmarked = isBookmarked,
                        onHide = onHide,
                        onToggleBookmark = onToggleBookmark?.let { toggle ->
                            {
                                toggle()
                                val message = if (isBookmarked) "Unbookmarked" else "Bookmarked"
                                snackbarHostState?.let { hostState ->
                                    coroutineScope.launch { hostState.showSnackbar(message) }
                                }
                            }
                        },
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }

            // Top-right badge stack: the external-link indicator above the expand chevron.
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 7.dp, end = 14.dp),
            ) {
                if (showExternalLinkIndicator) {
                    CardBadge(glyph = "→", onClick = onClick)
                    Box(modifier = Modifier.height(2.dp))
                }
                CardBadge(
                    glyph = "⌄",
                    onClick = { expanded = !expanded },
                    rotation = chevronRotation,
                )
            }
        }
    }
}

private const val CARD_EXPAND_COLLAPSE_DURATION_MS = 300

/**
 * The shared single-glyph badge used for every small action on the card. `rotation` lets
 * the caller animate the chevron.
 */
@Composable
internal fun CardBadge(glyph: String, onClick: () -> Unit, rotation: Float = 0f) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        onClick = onClick,
    ) {
        Text(
            glyph,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = MaterialTheme.typography.titleMedium.fontSize * 1.67f,
            ),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp).rotate(rotation),
        )
    }
}
