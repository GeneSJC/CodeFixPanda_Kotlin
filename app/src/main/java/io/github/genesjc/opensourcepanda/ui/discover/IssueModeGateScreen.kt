package io.github.genesjc.opensourcepanda.ui.discover

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.genesjc.opensourcepanda.R
import io.github.genesjc.opensourcepanda.ui.components.BrandTitleBand
import io.github.genesjc.opensourcepanda.ui.components.IndependentNotice
import io.github.genesjc.opensourcepanda.ui.components.LabelInfoDialog
import io.github.genesjc.opensourcepanda.model.IssueLabelFilter
import io.github.genesjc.opensourcepanda.ui.buttonLabelRes
import io.github.genesjc.opensourcepanda.ui.theme.OpenSourcePandaTheme
import io.github.genesjc.opensourcepanda.ui.theme.colorScheme

/**
 * The Issue Mode gate, shown by DiscoverNavHost whenever no mode is chosen: a literal gate, not a
 * silent default. One card per IssueLabelFilter entry, so a new mode appears here once it is added
 * to the enum and given a color scheme. The colored buttons are the only thing that selects a mode;
 * the screen itself does not switch the app theme, since no mode is chosen yet. An info icon beside
 * the prompt opens LabelInfoDialog for a user who wants to read what the modes mean before choosing.
 * The content sits in a nested theme with issueMode = null: tapping a card sets the mode
 * synchronously, and without the wrapper this screen's text color snapped to the new mode's tone just
 * before the transition. More in docs/design-notes.md, "Mode gate".
 */
@Composable
fun IssueModeGateScreen(onModeSelected: (IssueLabelFilter) -> Unit) {
    val slots = rememberModeButtonSlots()
    var showInfo by rememberSaveable { mutableStateOf(false) }
    val infoLabel = stringResource(R.string.label_info_title)
    OpenSourcePandaTheme(issueMode = null) {
        Scaffold(
            topBar = { BrandTitleBand() },
        ) { padding ->
            Box(
                modifier = Modifier.padding(padding).fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 16.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.gate_headline),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp),
                    )
                    // Right under the headline, so it is seen before anything is chosen.
                    IndependentNotice()
                    Row(
                        modifier = Modifier.padding(top = 24.dp, bottom = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            stringResource(R.string.gate_prompt),
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        // The tap area is larger than the glyph, so the icon is easy to hit.
                        Text(
                            "ⓘ",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clickable(onClickLabel = infoLabel) { showInfo = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        IssueLabelFilter.entries.forEach { mode ->
                            ModeButton(mode = mode, slots = slots, onSelect = { onModeSelected(mode) })
                        }
                    }
                }
            }
        }
        if (showInfo) {
            LabelInfoDialog(onDismiss = { showInfo = false })
        }
    }
}

@Composable
private fun ModeButton(mode: IssueLabelFilter, slots: ModeButtonSlots, onSelect: () -> Unit) {
    // Every button fills the same width, so they match; a long name wraps onto a second line.
    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(8.dp),
        color = mode.colorScheme().primary,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = MODE_BUTTON_SIDE_PADDING, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.width(slots.icon), contentAlignment = Alignment.Center) {
                Text(
                    // The text-presentation selector keeps a device from swapping in a
                    // colored emoji glyph, which would ignore the white.
                    mode.glyph + TEXT_PRESENTATION_SELECTOR,
                    style = modeButtonTextStyle(),
                    color = Color.White,
                )
            }
            Spacer(modifier = Modifier.width(MODE_ICON_TEXT_GAP))
            // Start-aligned, so the first letters of the names line up.
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                Text(
                    stringResource(mode.buttonLabelRes()),
                    style = modeButtonTextStyle(),
                    color = Color.White,
                )
            }
        }
    }
}

private const val TEXT_PRESENTATION_SELECTOR = "\uFE0E"

/** The icon and the name share this text size, 25% above titleMedium. */
@Composable
private fun modeButtonTextStyle() = MaterialTheme.typography.titleMedium.let {
    it.copy(fontSize = it.fontSize * 1.25f, lineHeight = it.lineHeight * 1.25f)
}

/** The width of the icon slot: the widest icon of any mode, shared by every button. */
private class ModeButtonSlots(val icon: Dp)

@Composable
private fun rememberModeButtonSlots(): ModeButtonSlots {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val style = modeButtonTextStyle()
    return remember(style, density) {
        fun widestPx(text: (IssueLabelFilter) -> String) =
            IssueLabelFilter.entries.maxOf { textMeasurer.measure(text(it), style).size.width }
        with(density) {
            ModeButtonSlots(icon = widestPx { it.glyph + TEXT_PRESENTATION_SELECTOR }.toDp())
        }
    }
}

private val MODE_BUTTON_SIDE_PADDING = 16.dp

// Half the 24dp this started at.
private val MODE_ICON_TEXT_GAP = 10.dp
