package io.github.genesjc.opensourcepanda.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.genesjc.opensourcepanda.R
import io.github.genesjc.opensourcepanda.model.RateLimitInfo
import io.github.genesjc.opensourcepanda.ui.text.resetsInText
import io.github.genesjc.opensourcepanda.ui.theme.RateLimitAmber
import io.github.genesjc.opensourcepanda.ui.theme.RateLimitGreen
import io.github.genesjc.opensourcepanda.ui.theme.RateLimitRed
import io.github.genesjc.opensourcepanda.ui.theme.RateLimitYellow
import kotlin.math.cos
import kotlin.math.sin

/** How much bigger the rate-limit chip is than its original size (5% larger; it was 50% larger before being shrunk 30%). */
private const val RATE_LIMIT_CHIP_SCALE = 1.05f

/**
 * Tappable rate-limit indicator as a rounded square: the meter icon over the remaining count over the limit,
 * like a fraction, so it reads top to bottom and takes only [RATE_WIDGET_WIDTH] of a header's width. Always
 * rendered (a dash over a dash when there are no numbers yet) so it does not pop in. Opens the dial dialog. It
 * has no margin of its own: the caller places it.
 */
@Composable
internal fun RateLimitWidget(info: RateLimitInfo?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val base = MaterialTheme.typography.labelMedium
    val color = MaterialTheme.colorScheme.onSecondaryContainer
    val shape = RoundedCornerShape(RATE_WIDGET_CORNER)
    // Tappable through clickable and not Surface(onClick), which would pad the widget out to Material's 48 dp touch target.
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier.size(width = RATE_WIDGET_WIDTH, height = RATE_WIDGET_HEIGHT).clip(shape).clickable(onClick = onClick),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            RateLimitMeterIcon()
            // Normally supplied with the first list (a live search, or a /rate_limit lookup after a cache hit); the
            // dashes show only briefly or if that lookup fails.
            Text(
                info?.remaining?.toString() ?: "—",
                style = base.copy(fontWeight = FontWeight.SemiBold, lineHeight = base.fontSize * 1.1f),
                color = color,
            )
            Box(modifier = Modifier.width(RATE_WIDGET_RULE_WIDTH).height(1.dp).background(color.copy(alpha = RATE_WIDGET_RULE_ALPHA)))
            Text(
                info?.limit?.toString() ?: "—",
                style = MaterialTheme.typography.labelSmall.let { it.copy(lineHeight = it.fontSize * 1.1f) },
                color = color,
            )
        }
    }
}

/** The widget's size and corner: a square-ish tile that fits the narrow column beside a header's title. */
private val RATE_WIDGET_WIDTH = 48.dp
private val RATE_WIDGET_HEIGHT = 52.dp
private val RATE_WIDGET_CORNER = 12.dp
private val RATE_WIDGET_RULE_WIDTH = 18.dp
private const val RATE_WIDGET_RULE_ALPHA = 0.45f

/**
 * The widget that stands where [RateLimitWidget] would while the demo data is shown: with no token there are no
 * rate-limit numbers, so it says "Demo data" in the same rounded square. With an [onClick] it is tappable (Top
 * Repos explains the missing token).
 */
@Composable
internal fun DemoWidget(onClick: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(RATE_WIDGET_CORNER)
    val tapModifier = if (onClick != null) modifier.clip(shape).clickable(onClick = onClick) else modifier
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = tapModifier.size(width = RATE_WIDGET_WIDTH, height = RATE_WIDGET_HEIGHT),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                stringResource(R.string.demo_chip_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * The chip that stands where the rate-limit widget would while the demo data is shown: with no token there
 * are no rate-limit numbers, so it says "Demo data" instead. A chip, for the places a chip fits (the repo screen's bar); the Top Repos header uses [DemoWidget]. With
 * an [onClick] it is tappable (Top Repos explains the missing token); without one it is just a label. Like
 * [RateLimitWidget] it has no margin of its own: the caller places it.
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
    val tapModifier = if (onClick != null) chipModifier.clip(shape).clickable(onClick = onClick) else chipModifier
    Surface(shape = shape, color = color, modifier = tapModifier, content = label)
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
