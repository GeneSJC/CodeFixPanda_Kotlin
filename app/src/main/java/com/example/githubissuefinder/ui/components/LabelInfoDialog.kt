package com.example.githubissuefinder.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.githubissuefinder.R
import com.example.githubissuefinder.model.IssueLabelFilter

/**
 * The "what do these tags mean?" dialog for "good first issue" and "help wanted". The wording comes
 * from IssueLabelFilter.description, shared with the gate screen's cards so they cannot drift apart.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_got_it)) }
        },
        title = { Text(stringResource(R.string.label_info_title)) },
        text = {
            Column {
                IssueLabelFilter.entries.forEach { mode ->
                    Text(
                        mode.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        mode.description,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
                    )
                }
                Text(
                    stringResource(R.string.label_info_footer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}
