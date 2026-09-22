package com.example.githubissuefinder.ui.components

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
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
 *
 * v0.47 ("share to yourself"): every card now ends in a Share +
 * Copy Link action row. Deliberately a SHARED addition, not scoped to
 * just RepoIssuesScreen where this whole conversation had been
 * focused — IssueCard is used by Feed too, and "send myself this
 * issue to work on later" is just as real a need from a mixed
 * cross-repo list as from a repo-scoped one.
 *
 * REVISED within the same CM, before being confirmed committed: the
 * first draft put both actions behind a small "⋮" overflow menu in the
 * top-right corner — too small relative to the rest of the card, per
 * direct feedback comparing it against the "↗" badge's size. Replaced
 * with two real, always-visible buttons in their own row at the
 * bottom of the card — a standard Material card pattern (content, then
 * an actions row), not a custom invention. Both are plain
 * androidx.compose.material3.Button/OutlinedButton — the SAME
 * components already used elsewhere in this app for Load More, Check
 * for More Issues, and Retry — deliberately reused rather than
 * hand-rolled, which resolves every part of the request at once:
 * genuinely bigger than the arrow badge by construction, pixel-
 * identical typography to every other button in the app because it
 * IS that same component, and a color (primary/onPrimary for Share,
 * an outlined primary-tinted style for Copy Link) already distinct
 * from both the "↗" badge's secondaryContainer and the GitHub-colored
 * label chips — all from the components' own sensible defaults, no
 * custom styling needed to achieve any of it.
 *
 * Share is filled (Button), Copy Link is outlined (OutlinedButton) —
 * a standard, common Material pairing for "primary action + secondary
 * action" sitting side by side, giving Share the slightly heavier
 * visual weight per the specific request to make IT more prominent,
 * without making Copy Link disappear entirely (it's still a full,
 * properly-sized button, just visually secondary to Share).
 *
 * WORTH KNOWING: promoting these from a subtle corner glyph to two
 * full buttons on every card adds real height and visual weight to
 * each card — a genuine tradeoff in a long list (Feed especially),
 * traded deliberately for the requested prominence. If this ever
 * reads as too heavy in a long scrolling list once seen running, a
 * reasonable middle ground would be showing this row only on tap/
 * long-press rather than always-visible — not attempted here since
 * "prominent" was the explicit, direct request.
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
 */
@Composable
fun IssueCard(
    issue: Issue,
    isBookmarked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showExternalLinkIndicator: Boolean = false,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

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

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
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
                    ) {
                        Text("Share")
                    }
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(issue.htmlUrl))
                            Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                        },
                    ) {
                        Text("Copy Link")
                    }
                }
            }

            if (showExternalLinkIndicator) {
                // TopEnd with an approximate top offset, NOT CenterEnd —
                // this badge was designed to sit next to the title
                // specifically (see the title's own matching end=56.dp
                // reservation above). CenterEnd centers relative to the
                // WHOLE card, which was fine when the card was shorter;
                // now that a Share/Copy Link row sits below the footer,
                // the card is taller, and CenterEnd would drift this
                // badge down into the labels or footer row instead of
                // staying by the title. TopEnd anchors to the top of the
                // card regardless of how much content follows below it.
                // The 46.dp offset is a reasonable-effort estimate of
                // where the title line actually starts (14.dp Column
                // padding + the identity row's own height above it), not
                // pixel-verified against a running device.
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 46.dp, end = 14.dp),
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
