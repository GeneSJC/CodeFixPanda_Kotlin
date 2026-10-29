package io.github.genesjc.opensourcepanda.ui.discover

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.genesjc.opensourcepanda.R
import io.github.genesjc.opensourcepanda.data.RateLimitInfo
import io.github.genesjc.opensourcepanda.ui.resetsInText
import io.github.genesjc.opensourcepanda.ui.theme.RateLimitAmber
import io.github.genesjc.opensourcepanda.ui.theme.RateLimitGreen
import io.github.genesjc.opensourcepanda.ui.theme.RateLimitRed
import io.github.genesjc.opensourcepanda.ui.theme.RateLimitYellow
import kotlin.math.cos
import kotlin.math.sin

/** How much bigger the rate-limit chip is than its original size (5% larger; it was 50% larger before being shrunk 30%). */
private const val RATE_LIMIT_CHIP_SCALE = 1.05f

/** The meter icon's size relative to its full-size drawing, shrunk with the chip's text (30% smaller). */
private const val RATE_LIMIT_ICON_SCALE = 0.7f

/**
 * Tappable rate-limit indicator: a meter icon and remaining/limit. Always rendered (a
 * placeholder when there are no numbers yet) so it does not pop in. Opens the dial dialog. It has no margin
 * of its own: the caller places it.
 */
@Composable
internal fun RateLimitChip(info: RateLimitInfo?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val base = MaterialTheme.typography.bodySmall
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape((12 * RATE_LIMIT_CHIP_SCALE).dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                horizontal = (10 * RATE_LIMIT_CHIP_SCALE).dp,
                vertical = (4 * RATE_LIMIT_CHIP_SCALE).dp,
            ),
        ) {
            RateLimitMeterIcon(scale = RATE_LIMIT_ICON_SCALE)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                // Normally supplied with the first list (a live search, or a /rate_limit lookup after a
                // cache hit); the placeholder shows only briefly or if that lookup fails.
                if (info != null) "${info.remaining}/${info.limit}" else "—",
                style = base.copy(
                    fontSize = base.fontSize * RATE_LIMIT_CHIP_SCALE,
                    lineHeight = base.lineHeight * RATE_LIMIT_CHIP_SCALE,
                ),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

/**
 * The chip that stands where the rate-limit chip would while the demo data is shown: with no token there
 * are no rate-limit numbers, so it says "Demo data" instead. Same size and shape as [RateLimitChip]. With
 * an [onClick] it is tappable (Top Repos explains the missing token); without one it is just a label. Like
 * [RateLimitChip] it has no margin of its own: the caller places it.
 */
@Composable
internal fun DemoChip(onClick: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    val base = MaterialTheme.typography.bodySmall
    val chipModifier = modifier
    val label: @Composable () -> Unit = {
        Text(
            stringResource(R.string.demo_chip_label),
            style = base.copy(
                fontSize = base.fontSize * RATE_LIMIT_CHIP_SCALE,
                lineHeight = base.lineHeight * RATE_LIMIT_CHIP_SCALE,
            ),
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(
                horizontal = (10 * RATE_LIMIT_CHIP_SCALE).dp,
                vertical = (4 * RATE_LIMIT_CHIP_SCALE).dp,
            ),
        )
    }
    val shape = RoundedCornerShape((12 * RATE_LIMIT_CHIP_SCALE).dp)
    val color = MaterialTheme.colorScheme.tertiaryContainer
    if (onClick != null) {
        Surface(onClick = onClick, shape = shape, color = color, modifier = chipModifier, content = label)
    } else {
        Surface(shape = shape, color = color, modifier = chipModifier, content = label)
    }
}

/**
 * A small static speedometer glyph: red, amber and green segments, a needle and a hub.
 * It says "meter" and nothing about the value, which the numbers beside it give. Drawn with
 * Canvas because thick segments stay legible at this size where an emoji does not.
 */
@Composable
private fun RateLimitMeterIcon(modifier: Modifier = Modifier, scale: Float = 1f) {
    val needleColor = MaterialTheme.colorScheme.onSecondaryContainer
    val description = stringResource(R.string.rate_limit_meter_description)
    Canvas(
        modifier = modifier
            .size(width = (24 * scale).dp, height = (15 * scale).dp)
            .semantics { contentDescription = description },
    ) {
        val strokeWidthPx = (4 * scale).dp.toPx()
        val hubRadiusPx = (2.5f * scale).dp.toPx()
        val radius = size.width / 2f - strokeWidthPx / 2f
        val center = Offset(size.width / 2f, size.height - hubRadiusPx)
        val arcTopLeft = Offset(center.x - radius, center.y - radius)
        val arcSize = Size(radius * 2f, radius * 2f)

        // Three 56-degree segments with 6-degree gaps across the top half.
        val segmentColors = listOf(RateLimitRed, RateLimitAmber, RateLimitGreen)
        segmentColors.forEachIndexed { index, color ->
            drawArc(
                color = color,
                startAngle = 180f + index * 62f + 3f,
                sweepAngle = 56f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx),
            )
        }

        // A fixed needle, pointing up and a little to the right.
        val needleAngleRad = Math.toRadians(305.0)
        val needleTip = Offset(
            center.x + (radius - (1 * scale).dp.toPx()) * cos(needleAngleRad).toFloat(),
            center.y + (radius - (1 * scale).dp.toPx()) * sin(needleAngleRad).toFloat(),
        )
        drawLine(
            color = needleColor,
            start = center,
            end = needleTip,
            strokeWidth = (2 * scale).dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawCircle(color = needleColor, radius = hubRadiusPx, center = center)
    }
}

/** Text below the big number in the rate-limit dialog is 50% larger. */
private const val RATE_LIMIT_DIALOG_TEXT_SCALE = 1.5f

/** This style with its font size and line height scaled by [RATE_LIMIT_DIALOG_TEXT_SCALE]. */
private fun TextStyle.rateLimitDialogScaled(): TextStyle = copy(
    fontSize = fontSize * RATE_LIMIT_DIALOG_TEXT_SCALE,
    lineHeight = lineHeight * RATE_LIMIT_DIALOG_TEXT_SCALE,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RateLimitDialog(info: RateLimitInfo?, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
        title = { Text(stringResource(R.string.rate_limit_dialog_title)) },
        text = {
            if (info == null) {
                Text(stringResource(R.string.rate_limit_no_data))
            } else {
                // Scrollable because the larger text can make the dialog
                // taller than the screen, e.g. in landscape.
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    RateLimitDial(info = info)
                    RateLimitRow(stringResource(R.string.rate_limit_remaining), "${info.remaining}")
                    RateLimitRow(stringResource(R.string.rate_limit_limit), "${info.limit}")
                    RateLimitRow(stringResource(R.string.rate_limit_used), "${info.used}")
                    RateLimitRow(stringResource(R.string.rate_limit_resets_in), resetsInText(info.resetsInSeconds))
                    Text(
                        stringResource(R.string.rate_limit_search_note),
                        style = MaterialTheme.typography.bodySmall.rateLimitDialogScaled(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                    Text(
                        stringResource(R.string.rate_limit_window_note),
                        style = MaterialTheme.typography.bodySmall.rateLimitDialogScaled(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
    )
}

/**
 * Hand-drawn semicircular dial: a fixed red-to-green track with a needle at the current
 * remaining/limit ratio. A needle, not a knob, because the gauge is display-only. The text
 * sits below the Canvas as ordinary Text, which lays out more simply than text in the arc.
 */
@Composable
private fun RateLimitDial(info: RateLimitInfo, modifier: Modifier = Modifier) {
    val ratio = (info.remaining.toFloat() / info.limit.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val needleColor = MaterialTheme.colorScheme.onSurface

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        val gaugeDescription = stringResource(R.string.rate_limit_gauge_description, info.remaining, info.limit)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .semantics { contentDescription = gaugeDescription },
        ) {
            val strokeWidthPx = 20.dp.toPx()
            val radius = (minOf(size.width / 2f, size.height) - strokeWidthPx / 2f).coerceAtLeast(0f)
            val center = Offset(size.width / 2f, size.height - strokeWidthPx / 2f)
            val arcTopLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2f, radius * 2f)

            // Fixed track: red at 180° (left, "0") through orange and yellow to green at 0° (right,
            // "max"). Only the visible half is drawn.
            val trackBrush = Brush.sweepGradient(
                0.0f to RateLimitGreen,
                0.5f to RateLimitRed,
                0.667f to RateLimitAmber,
                0.833f to RateLimitYellow,
                1.0f to RateLimitGreen,
                center = center,
            )

            drawArc(
                brush = trackBrush,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
            )

            // Pointer position: ratio=0 (empty) -> 180° (left/red).
            // ratio=1 (full) -> 360°/0° (right/green).
            val needleAngleRad = Math.toRadians((180f + 180f * ratio).toDouble())
            val needleTip = Offset(
                center.x + radius * cos(needleAngleRad).toFloat(),
                center.y + radius * sin(needleAngleRad).toFloat(),
            )

            drawLine(
                color = needleColor,
                start = center,
                end = needleTip,
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawCircle(color = needleColor, radius = 8.dp.toPx(), center = center)
        }

        Text("${info.remaining}", style = MaterialTheme.typography.headlineMedium)
        Text(
            stringResource(R.string.rate_limit_remaining),
            style = MaterialTheme.typography.bodySmall.rateLimitDialogScaled(),
            color = onSurfaceVariant,
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("0", style = MaterialTheme.typography.labelSmall.rateLimitDialogScaled(), color = onSurfaceVariant)
            Text(
                "${info.limit}",
                style = MaterialTheme.typography.labelSmall.rateLimitDialogScaled(),
                color = onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RateLimitRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium.rateLimitDialogScaled())
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.rateLimitDialogScaled(),
            fontWeight = FontWeight.Medium,
        )
    }
}
