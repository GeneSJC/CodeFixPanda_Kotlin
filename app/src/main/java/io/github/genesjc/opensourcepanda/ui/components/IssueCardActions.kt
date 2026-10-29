package io.github.genesjc.opensourcepanda.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import io.github.genesjc.opensourcepanda.R
import io.github.genesjc.opensourcepanda.model.Issue
import io.github.genesjc.opensourcepanda.ui.relativeTimeText

/**
 * The row an expanded issue card reveals: the action badges (hide, bookmark, share, copy link)
 * and the comment count with the updated time. Hide and bookmark appear only when their callback
 * is given. The bookmark callback arrives already wrapped with its Snackbar message by the card,
 * which owns the scope, since this row leaves composition when the card collapses. Share and
 * Copy Link need nothing from the caller.
 */
@Composable
internal fun IssueCardActions(
    issue: Issue,
    isBookmarked: Boolean,
    onHide: (() -> Unit)?,
    onToggleBookmark: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val shareChooserTitle = stringResource(R.string.share_issue_chooser)
    val linkCopiedMessage = stringResource(R.string.link_copied)

    Row(
        modifier = modifier.fillMaxWidth(),
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
                CardBadge(glyph = if (isBookmarked) "★" else "☆", onClick = onToggleBookmark, highlighted = isBookmarked)
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
                    context.startActivity(Intent.createChooser(shareIntent, shareChooserTitle))
                },
            )
            CardBadge(
                glyph = "📋",
                onClick = {
                    clipboardManager.setText(AnnotatedString(issue.htmlUrl))
                    Toast.makeText(context, linkCopiedMessage, Toast.LENGTH_SHORT).show()
                },
            )
        }

        Text(
            text = stringResource(
                R.string.comments_and_updated,
                pluralStringResource(R.plurals.comment_count, issue.commentCount, issue.commentCount),
                relativeTimeText(issue.updatedAtMs),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
