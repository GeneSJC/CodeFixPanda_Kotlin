package com.example.githubissuefinder.ui.components

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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.githubissuefinder.model.Issue

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
                CardBadge(glyph = if (isBookmarked) "★" else "☆", onClick = onToggleBookmark)
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
