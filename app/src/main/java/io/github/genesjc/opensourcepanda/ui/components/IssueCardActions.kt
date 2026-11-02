package io.github.genesjc.opensourcepanda.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import io.github.genesjc.opensourcepanda.R
import io.github.genesjc.opensourcepanda.model.Issue
import io.github.genesjc.opensourcepanda.ui.text.relativeTimeText

/**
 * The panel an expanded issue card reveals: the comment count with the updated time and a copy-link badge, and
 * under them labeled save, share and hide buttons. Hide and save appear only when their callback is given. The
 * bookmark callback arrives already wrapped with its Snackbar message by the card, which owns the scope, since
 * this panel leaves composition when the card collapses. Share and Copy Link need nothing from the caller.
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_chat_bubble),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
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
            CardBadge(
                icon = R.drawable.ic_link,
                contentDescription = stringResource(R.string.action_copy_link),
                onClick = {
                    clipboardManager.setText(AnnotatedString(issue.htmlUrl))
                    Toast.makeText(context, linkCopiedMessage, Toast.LENGTH_SHORT).show()
                },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (onToggleBookmark != null) {
                ActionButton(
                    icon = if (isBookmarked) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark,
                    label = stringResource(if (isBookmarked) R.string.action_saved else R.string.action_save),
                    onClick = onToggleBookmark,
                    highlighted = isBookmarked,
                    modifier = Modifier.weight(1f),
                )
            }
            ActionButton(
                icon = R.drawable.ic_share,
                label = stringResource(R.string.action_share),
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
                modifier = Modifier.weight(1f),
            )
            if (onHide != null) {
                ActionButton(
                    icon = R.drawable.ic_visibility_off,
                    label = stringResource(R.string.action_hide),
                    onClick = onHide,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * A labeled action in the panel: an icon and a word in an outlined rounded button, [highlighted] in the seafoam
 * for a saved state. Clickable and not Surface(onClick), so it stays the 36 dp it looks.
 */
@Composable
private fun ActionButton(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
) {
    val shape = RoundedCornerShape(10.dp)
    Surface(
        shape = shape,
        color = if (highlighted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surface,
        contentColor = if (highlighted) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, if (highlighted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline),
        modifier = modifier.height(36.dp).clip(shape).clickable(onClick = onClick),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}
