package io.github.genesjc.opensourcepanda.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.genesjc.opensourcepanda.model.IssueLabel

/**
 * Renders one GitHub label as a quiet chip: the label's name in the app's text color on a plain outlined
 * chip, with a small dot in the label's real hex color from the API. GitHub's label colors span the full
 * range (and clash with the app's palette when used as the whole chip), so the color is a dot and the chip
 * itself belongs to the app. A label whose color cannot be read gets a dot in the outline color.
 */
@Composable
fun LabelChip(label: IssueLabel, modifier: Modifier = Modifier) {
    val dotColor = remember(label.colorHex) { parseHexColor(label.colorHex) } ?: MaterialTheme.colorScheme.outline

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(start = 8.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = dotColor,
                modifier = Modifier.size(8.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            ) {}
            Text(
                text = label.name,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

private fun parseHexColor(hex: String): Color? = runCatching {
    Color(android.graphics.Color.parseColor("#$hex"))
}.getOrNull()
