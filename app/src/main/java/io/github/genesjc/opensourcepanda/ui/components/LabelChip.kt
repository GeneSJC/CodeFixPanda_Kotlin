package io.github.genesjc.opensourcepanda.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import io.github.genesjc.opensourcepanda.model.IssueLabel

/**
 * Renders one GitHub label using its real hex color from the API (or, for
 * now, our hardcoded sample data). Text color flips between black and
 * white based on the background's luminance, since GitHub label colors
 * span the full brightness range and a fixed text color would be
 * unreadable on some of them.
 */
@Composable
fun LabelChip(label: IssueLabel, modifier: Modifier = Modifier) {
    val backgroundColor = remember(label.colorHex) { parseHexColor(label.colorHex) }
    val textColor = if (backgroundColor.luminance() > 0.5f) Color.Black else Color.White

    Text(
        text = label.name,
        color = textColor,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

private fun parseHexColor(hex: String): Color = runCatching {
    Color(android.graphics.Color.parseColor("#$hex"))
}.getOrDefault(Color.Gray)
