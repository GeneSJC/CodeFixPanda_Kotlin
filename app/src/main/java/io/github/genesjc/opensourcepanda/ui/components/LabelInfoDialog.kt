package io.github.genesjc.opensourcepanda.ui.components

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
import io.github.genesjc.opensourcepanda.R
import io.github.genesjc.opensourcepanda.model.IssueLabelFilter
import io.github.genesjc.opensourcepanda.ui.text.descriptionRes
import io.github.genesjc.opensourcepanda.ui.text.nameRes

/**
 * The "what do these two choices mean?" dialog for the beginner-friendly and asking-for-help modes. The wording comes
 * from IssueLabelFilter.descriptionRes(). It opens from the info icon on the mode gate and from the pill on Top Repos.
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
                        stringResource(mode.nameRes()),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(mode.descriptionRes()),
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
