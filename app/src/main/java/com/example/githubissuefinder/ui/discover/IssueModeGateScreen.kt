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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    GitHubIssueFinderTheme(issueMode = null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text("GitHubIssueFinder", style = MaterialTheme.typography.titleLarge)
                    },
                )
            },
        ) { padding ->
            Box(
                modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "What are you looking for?",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "Pick a mode to focus the whole app around — you can switch again anytime from the menu at the top.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp, bottom = 28.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        IssueLabelFilter.entries.forEach { mode ->
                            ModeCard(mode = mode, onSelect = { onModeSelected(mode) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeCard(mode: IssueLabelFilter, onSelect: () -> Unit) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = onSelect,
                    shape = RoundedCornerShape(8.dp),
                    color = accent,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            mode.glyph,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Text(
                            mode.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "⌄",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
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
