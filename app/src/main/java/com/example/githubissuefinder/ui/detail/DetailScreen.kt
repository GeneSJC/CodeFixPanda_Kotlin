package com.example.githubissuefinder.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.githubissuefinder.model.Issue
import com.example.githubissuefinder.ui.components.LabelChip

/**
 * Full issue detail. "Open in GitHub" launches a standard ACTION_VIEW
 * Intent against issue.htmlUrl — a plain public URL, not a custom deep
 * link. Android decides whether that opens the GitHub app or a browser;
 * this code has no say in — or need to know — which one handles it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    issue: Issue?,
    isBookmarked: Boolean,
    onBack: () -> Unit,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(issue?.repoFullName.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", style = MaterialTheme.typography.titleMedium)
                    }
                },
            )
        },
    ) { padding ->
        if (issue == null) {
            Text(
                "Issue not found.",
                modifier = Modifier.padding(padding).padding(16.dp),
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "Updated ${issue.updatedAtLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = issue.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                issue.labels.forEach { label -> LabelChip(label) }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                text = issue.bodyText,
                style = MaterialTheme.typography.bodyMedium,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (isBookmarked) "★ Saved" else "☆ Save")
                }
                Button(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(issue.htmlUrl)))
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Open in GitHub")
                }
            }
        }
    }
}
