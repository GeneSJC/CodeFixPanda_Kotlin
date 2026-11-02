package io.github.genesjc.opensourcepanda.ui.modegate

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.genesjc.opensourcepanda.R
import io.github.genesjc.opensourcepanda.ui.components.BrandSoftBackground
import io.github.genesjc.opensourcepanda.ui.components.BrandTitleBand
import io.github.genesjc.opensourcepanda.ui.components.IndependentNotice
import io.github.genesjc.opensourcepanda.ui.components.LabelInfoDialog
import io.github.genesjc.opensourcepanda.model.IssueLabelFilter
import io.github.genesjc.opensourcepanda.ui.text.buttonLabelRes
import io.github.genesjc.opensourcepanda.ui.text.glyph
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
    var showInfo by rememberSaveable { mutableStateOf(false) }
    val infoLabel = stringResource(R.string.label_info_title)
    OpenSourcePandaTheme(issueMode = null) {
        BrandSoftBackground {
            Scaffold(
                containerColor = Color.Transparent,
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
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .clickable(onClickLabel = infoLabel) { showInfo = true }
                                    .padding(horizontal = INFO_ICON_SIDE_PADDING, vertical = 8.dp),
                            )
                        }
                        // The two choices side by side, equally wide and as tall as the taller one.
                        Row(
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(MODE_BUTTON_GAP),
                        ) {
                            IssueLabelFilter.entries.forEach { mode ->
                                ModeButton(
                                    mode = mode,
                                    onSelect = { onModeSelected(mode) },
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                )
                            }
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
private fun ModeButton(mode: IssueLabelFilter, onSelect: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = mode.colorScheme()
    // 25% larger than titleMedium, size and line height together.
    val nameStyle = MaterialTheme.typography.titleMedium.let {
        it.copy(fontSize = it.fontSize * MODE_BUTTON_TEXT_SCALE, lineHeight = it.lineHeight * MODE_BUTTON_TEXT_SCALE)
    }
    // The icon above the name, both centered; a long name wraps onto a second line.
    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(8.dp),
        color = scheme.primary,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = MODE_BUTTON_SIDE_PADDING, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                // The text-presentation selector keeps a device from swapping in a
                // colored emoji glyph, which would ignore the text color.
                mode.glyph() + TEXT_PRESENTATION_SELECTOR,
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onPrimary,
            )
            Text(
                stringResource(mode.buttonLabelRes()),
                style = nameStyle,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = scheme.onPrimary,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

private const val TEXT_PRESENTATION_SELECTOR = "\uFE0E"

private const val MODE_BUTTON_TEXT_SCALE = 1.25f

private val MODE_BUTTON_SIDE_PADDING = 14.dp
private val MODE_BUTTON_GAP = 12.dp

/** The info icon's tap area is larger than the glyph; the side margin was 12 dp and is 20% less. */
private val INFO_ICON_SIDE_PADDING = 9.6.dp
