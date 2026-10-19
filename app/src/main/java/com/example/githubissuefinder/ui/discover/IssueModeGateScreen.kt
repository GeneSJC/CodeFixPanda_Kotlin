package com.example.githubissuefinder.ui.discover

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.githubissuefinder.R
import com.example.githubissuefinder.model.IssueLabelFilter
import com.example.githubissuefinder.ui.theme.GitHubIssueFinderTheme
import com.example.githubissuefinder.ui.theme.colorScheme

/**
 * The Issue Mode gate, shown by DiscoverNavHost whenever no mode is chosen: a literal gate, not a
 * silent default. One card per IssueLabelFilter entry, so a new mode appears here once it is added
 * to the enum and given a color scheme. Each card is tinted with a light hint of its mode's color;
 * the screen itself does not switch the app theme, since no mode is chosen yet. The colored pill is
 * the only thing that selects a mode; a chevron toggles an inline description (the same wording as
 * LabelInfoDialog) so a user can read what a mode means before committing.
 * The content sits in a nested theme with issueMode = null: tapping a card sets the mode
 * synchronously, and without the wrapper this screen's text color snapped to the new mode's tone just
 * before the transition. More in docs/design-notes.md, "Mode gate".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueModeGateScreen(onModeSelected: (IssueLabelFilter) -> Unit) {
    val slots = rememberModeButtonSlots()
    GitHubIssueFinderTheme(issueMode = null) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                    },
                )
            },
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
                    Text(
                        stringResource(R.string.gate_prompt),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp, bottom = 40.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        IssueLabelFilter.entries.forEach { mode ->
                            ModeCard(mode = mode, slots = slots, onSelect = { onModeSelected(mode) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeCard(mode: IssueLabelFilter, slots: ModeButtonSlots, onSelect: () -> Unit) {
    val accent = mode.colorScheme().primary
    var expanded by rememberSaveable { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(220),
        label = "modeDescriptionChevronRotation",
    )
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.10f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                // One button, centered in the card: the icon and the name each sit in a slot as wide
                // as the widest of any mode, so both buttons are the same width and symmetric.
                Surface(
                    onClick = onSelect,
                    shape = RoundedCornerShape(8.dp),
                    color = accent,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = MODE_BUTTON_SIDE_PADDING, vertical = 10.dp),
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
                        Box(modifier = Modifier.width(slots.text), contentAlignment = Alignment.CenterStart) {
                            Text(
                                mode.buttonLabel(),
                                style = modeButtonTextStyle(),
                                color = Color.White,
                            )
                        }
                    }
                }
                Text(
                    "⌄",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(start = 8.dp)
                        .rotate(chevronRotation)
                        .clickable { expanded = !expanded },
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(220)),
                exit = shrinkVertically(animationSpec = tween(220)) + fadeOut(animationSpec = tween(220)),
            ) {
                Text(
                    mode.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

/** The mode name with each word capitalized, for the button ("Good First Issue"). */
private fun IssueLabelFilter.buttonLabel() =
    displayName.split(" ").joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

private const val TEXT_PRESENTATION_SELECTOR = "\uFE0E"

/** The icon and the name share this text size, 25% above titleMedium. */
@Composable
private fun modeButtonTextStyle() = MaterialTheme.typography.titleMedium.let {
    it.copy(fontSize = it.fontSize * 1.25f, lineHeight = it.lineHeight * 1.25f)
}

/** The widths of the icon slot and the name slot: the widest of any mode, shared by every button. */
private class ModeButtonSlots(val icon: Dp, val text: Dp)

@Composable
private fun rememberModeButtonSlots(): ModeButtonSlots {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val style = modeButtonTextStyle()
    return remember(style, density) {
        fun widestPx(text: (IssueLabelFilter) -> String) =
            IssueLabelFilter.entries.maxOf { textMeasurer.measure(text(it), style).size.width }
        with(density) {
            ModeButtonSlots(
                icon = widestPx { it.glyph + TEXT_PRESENTATION_SELECTOR }.toDp(),
                text = widestPx { it.buttonLabel() }.toDp(),
            )
        }
    }
}

private val MODE_BUTTON_SIDE_PADDING = 14.dp

// Half the 24dp this started at.
private val MODE_ICON_TEXT_GAP = 12.dp
