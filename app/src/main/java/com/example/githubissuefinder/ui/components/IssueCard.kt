package com.example.githubissuefinder.ui.components

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.githubissuefinder.model.Issue
import kotlinx.coroutines.launch

/**
 * The core issue card. `isBookmarked` is passed in rather than read from the
 * Issue model, since bookmark state lives separately (see
 * RepoBookmarks.kt) — this keeps Issue immutable and the bookmark state
 * as the single source of truth.
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
 *
 * v0.47 ("share to yourself"): every card gained a Share + Copy Link
 * action pair. ORIGINALLY shipped as two full Button/OutlinedButton
 * components in their own row at the card's bottom — see the density
 * pass below for why that's no longer the current shape. Kept here as
 * real history, not deleted: the original reasoning (why real buttons
 * over a small "⋮" overflow menu, why Share filled vs. Copy Link
 * outlined for relative prominence) genuinely informed the badge
 * sizing/coloring choices the current CardBadge treatment still uses.
 *
 * "Share" is a plain Intent.ACTION_SEND wrapped in
 * Intent.createChooser — the system share sheet, not a hardcoded
 * "email this" button. This genuinely subsumes email rather than
 * competing with it: Gmail shows up in that same sheet as one option
 * among many (Slack, Keep, a messaging app, whatever's actually
 * installed), for the same one Intent call — the exact same pattern
 * IssueCard's own external-link indicator already uses
 * (Intent.ACTION_VIEW), just a different action. "Copy Link" uses
 * Compose's LocalClipboardManager directly and confirms with a plain
 * system Toast — deliberately NOT a Snackbar, since that would need
 * every screen embedding this shared card to add its own SnackbarHost
 * just to support a confirmation this component triggers; a Toast is
 * fully self-contained and needs nothing from the caller.
 *
 * Both actions share text built from issue.repoFullName + issue.title
 * + issue.htmlUrl — giving whoever receives it (very possibly a future
 * version of the same person, at a computer) enough context to know
 * what the issue actually is without needing to already be looking at
 * the card it came from.
 *
 * v0.48 (repo-first bookmarking): a bookmark toggle — a small "★"/"☆"
 * badge, styled to match the "↗" external-link badge's exact size and
 * shape (same Surface + RoundedCornerShape + enlarged glyph treatment)
 * — is OPTIONAL, controlled by onToggleBookmark being non-null.
 * RepoIssuesScreen and RepoBookmarksScreen provide it, backed by the
 * repo-first flow's RepoBookmarks store (see that file's own doc
 * comment).
 *
 * Tapping it calls onToggleBookmark(), then shows a real Snackbar
 * ("Bookmarked" / "Unbookmarked") via the caller-supplied
 * snackbarHostState — a genuine departure from Copy Link's Toast a few
 * paragraphs up, and worth being direct about why: a Snackbar was the
 * explicit, specific request for this action, so it's honored here
 * rather than defaulting to the Toast pattern for consistency's own
 * sake. The cost is real, not hidden: unlike Copy Link, this action
 * requires whatever screen wants the bookmark toggle to own and supply
 * its own SnackbarHostState — RepoIssuesScreen gained one for exactly
 * this (it didn't have one before this CM), and the new
 * RepoBookmarksScreen has one from the start. Both actions living on
 * the same card, one via Toast and one via Snackbar, is a genuine
 * inconsistency worth knowing about rather than glossing over — kept
 * this way because it's what was actually asked for, not because it's
 * the tidier design.
 *
 * The message shown ("Bookmarked" vs "Unbookmarked") is predicted from
 * the CURRENT isBookmarked value this composable already has (not a
 * return value from onToggleBookmark, which stays a plain () -> Unit)
 * — safe because the toggle it triggers is synchronous in its
 * in-memory effect (see RepoBookmarks.toggle()'s own doc comment);
 * the disk write happens after, off the UI thread, and never blocks
 * what the user sees or reads here.
 *
 * v0.51 ("Hide Issue"): an OPTIONAL "✕" hide badge — same Surface/
 * RoundedCornerShape/enlarged-glyph treatment as the bookmark star and
 * the "↗" external-link badge, so it reads as the same family of
 * control — controlled by onHide being non-null. Feed and
 * RepoBookmarksScreen pass nothing new and are unaffected; only
 * RepoIssuesScreen supplies it. Deliberately a single fire-and-forget
 * callback, NOT paired with its own Snackbar here the way the
 * bookmark toggle is: the card that triggered it is about to be
 * removed from the list (that's the whole point of Hide), which would
 * cancel a coroutineScope tied to this composable before a
 * Snackbar-with-Undo could resolve. RepoIssuesScreen owns the
 * hide/undo Snackbar itself instead, with a scope that outlives any
 * one card. See that screen's own doc comment.
 *
 * DENSITY PASS (per direct feedback that one card took up too much
 * vertical space in a long list): four layout changes, all aimed at
 * reclaiming real height rather than just moving things around —
 *  - The identity row drops repoFullName entirely — repoDisplayTitle
 *    alone is enough to identify the repo on a screen (RepoIssuesScreen)
 *    that's already scoped to one repo, and Feed's own cards still get
 *    full identity context from the title below it. Losing a whole
 *    text line here is the single biggest height win in this pass.
 *  - The language dot+label (previously its own row, SpaceBetween
 *    against the comment-count text) now shares the labels row instead
 *    — language rendered at the END of that same Row, labels at the
 *    START. The whole row is skipped only when there are genuinely
 *    neither labels nor a language to show.
 *  - Share and Copy Link are no longer full Button/OutlinedButton
 *    components in their own bottom row — both are now CardBadge
 *    icons (📤 / 📋), matching the Hide/Bookmark/external-link badges'
 *    existing family exactly (same helper now, see CardBadge below).
 *    Share specifically moved OUT of the action row entirely and into
 *    a small vertical stack in the top-right corner, directly under
 *    the "↗" external-link badge — both are "leaves this card" actions
 *    (open in browser / hand off to someone else), so grouping them
 *    together reads more sensibly than splitting Share across two
 *    different corners of the card.
 *  - Hide, Bookmark, and Copy Link — what's left of the old bottom
 *    action row — now live in the SAME row as the comment-count/
 *    updated-at text, which used to share a row with the language
 *    indicator before language moved up to the labels row. Net effect:
 *    this card went from five content rows (identity, title, labels,
 *    language/comments, actions) down to three (identity, title,
 *    labels+language, actions+comments) plus the title's own top-right
 *    badge stack — the same information, no row purely for one glyph
 *    or one line of text anymore.
 *
 * DENSITY PASS, follow-up: the identity row above (repoDisplayTitle +
 * the "★ saved" tag) is now gone entirely, not just trimmed to one
 * line — on a per-repo screen the repo name is already redundant with
 * the screen it's on, and the bookmark star's own filled/outline state
 * on its CardBadge already conveys "saved" without a separate text
 * label. The title is now the card's first content row, full width.
 * The top-right badge stack's offset was adjusted to match: it now
 * aligns with the title row directly, since there's no identity row
 * above it to offset past.
 *
 * DENSITY PASS, second follow-up: a static "Kotlin" pill briefly lived
 * on every card, to the title's right (see history above/below this
 * paragraph's own prior revisions). It's gone again — this app
 * currently only ever searches Kotlin issues, so repeating that tag on
 * every single card in a list was genuinely redundant. It now appears
 * exactly ONCE per screen, as a static pill in RepoIssuesScreen's own
 * filter row (next to the active Issue Mode pill), not on this shared
 * card component at all. See RepoIssuesScreen's own doc comment.
 *
 * COLLAPSIBLE CARD PASS: the whole card is now an expand/collapse
 * widget. Only the title (plus the corner badge stack beside it) shows
 * by default; everything else — labels, Hide/Bookmark/Share/Copy Link,
 * the comment-count/updated-at line — is wrapped in an
 * AnimatedVisibility, hidden until the new expand-chevron badge is
 * tapped. `expanded` is plain per-composable `remember` state, not
 * hoisted to the caller — nothing outside this card needs to know or
 * control whether any given card is expanded, and RepoIssuesScreen's
 * `items(visibleIssues, key = { it.id })` already keys each card by
 * issue id, so each card's own expanded/collapsed state survives
 * ordinary recomposition correctly.
 *
 * The external-link badge's glyph changed from "↗" (diagonal) to "→"
 * (straight right) — it still does exactly what it always did (opens
 * the issue in GitHub via onClick), just a direct instruction to make
 * its direction read as forward/right rather than diagonal-out. The
 * Share badge moved out of the top-right corner stack and into the
 * (now-collapsible) actions row alongside Hide/Bookmark/Copy Link —
 * freeing that corner for the new expand-chevron badge, placed
 * directly beneath the "→" badge with a tight 4dp gap between them
 * (the same spacing Share's old corner slot used), per direct
 * instruction to keep the two squeezed together rather than spread
 * out. The chevron rotates 180° on toggle, using a quick 300ms
 * tween — deliberately NOT the README widget's 2-second "hero"
 * duration (ReadmeToggleButton/ReadmeExpandableBody), since this
 * triggers on every card in a scrolling list rather than being a
 * single deliberate action per screen; a 2-second animation repeated
 * per-card would read as sluggish rather than deliberate.
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
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
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

                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(animationSpec = tween(CARD_EXPAND_COLLAPSE_DURATION_MS)) +
                        fadeIn(animationSpec = tween(CARD_EXPAND_COLLAPSE_DURATION_MS)),
                    exit = shrinkVertically(animationSpec = tween(CARD_EXPAND_COLLAPSE_DURATION_MS)) +
                        fadeOut(animationSpec = tween(CARD_EXPAND_COLLAPSE_DURATION_MS)),
                ) {
                    Column {
                        if (issue.labels.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                issue.labels.take(3).forEach { label -> LabelChip(label) }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = if (issue.labels.isEmpty()) 10.dp else 0.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (onHide != null) {
                                    CardBadge(glyph = "✕", onClick = onHide)
                                }
                                if (onToggleBookmark != null) {
                                    CardBadge(
                                        glyph = if (isBookmarked) "★" else "☆",
                                        onClick = {
                                            onToggleBookmark()
                                            val message = if (isBookmarked) "Unbookmarked" else "Bookmarked"
                                            snackbarHostState?.let { hostState ->
                                                coroutineScope.launch { hostState.showSnackbar(message) }
                                            }
                                        },
                                    )
                                }
                                CardBadge(
                                    glyph = "📤",
                                    onClick = {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, issue.title)
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "${issue.repoFullName}: ${issue.title}\n${issue.htmlUrl}",
                                            )
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share issue"))
                                    },
                                )
                                CardBadge(
                                    glyph = "📋",
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(issue.htmlUrl))
                                        Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                                    },
                                )
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

            // Top-right badge stack: the external-link indicator (if
            // shown) directly above the expand/collapse chevron, squeezed
            // to a 2.dp gap per direct instruction — on a two-line title,
            // this keeps the whole stack's height close to the title's
            // own two-line height, so the chevron lands close to the
            // title's second line rather than floating below it (a
            // reasonable-effort estimate, not pixel-verified against a
            // running device). Top-aligned via the 14.dp offset, matching
            // the Column's own 14.dp padding, regardless of whether the
            // title wraps to one or two lines.
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 14.dp, end = 14.dp),
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
 * The shared small-glyph badge — Surface + RoundedCornerShape(8.dp) +
 * an enlarged (1.67x titleMedium) Text — used for every single-glyph
 * action on this card (Hide, Bookmark, Copy Link, Share, the external-
 * link indicator, the expand/collapse chevron). Extracted during the
 * density pass once Share and Copy Link joined this same visual family
 * instead of being full Button/OutlinedButton components — five
 * near-identical call sites was the signal to pull this out, not a
 * change made ahead of need.
 *
 * `rotation` (added for the expand/collapse chevron) is a plain degree
 * value applied via Modifier.rotate — the caller owns the actual
 * animation (animateFloatAsState), this composable just renders
 * whatever angle it's handed. Every other badge leaves it at the
 * default 0f.
 */
@Composable
private fun CardBadge(glyph: String, onClick: () -> Unit, rotation: Float = 0f) {
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
