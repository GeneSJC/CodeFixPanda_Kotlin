package com.example.githubissuefinder.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.githubissuefinder.model.IssueLabelFilter
import com.example.githubissuefinder.ui.theme.colorScheme

/**
 * The Issue Mode gate (v0.41): shown by DiscoverNavHost instead of the
 * normal repo-first flow whenever IssueMode.current is null — a real,
 * literal gate rather than a silent default to one mode or the other,
 * per how this was explicitly asked for ("you have to set it into the
 * mode you're looking for").
 *
 * One card per IssueLabelFilter.entries — NOT hardcoded to exactly two
 * cards for GFI/HW specifically, so a future third or fourth Mode (the
 * explicit "not limited to 2 or 3 or 4" goal) shows up here for free
 * the moment it's added to that enum and given a colorScheme() branch
 * in ModeColorSchemes.kt, with no change needed to this file.
 *
 * Each card is tinted with a light hint of that mode's own primary
 * color (at low alpha, against the screen's still-neutral default
 * background — this screen itself deliberately doesn't switch the app
 * theme, since no mode is chosen yet) — a small preview of what
 * you're about to switch into, not the full pastel background that
 * only applies once a mode is actually active.
 */
@Composable
fun IssueModeGateScreen(onModeSelected: (IssueLabelFilter) -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
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
                    ModeCard(mode = mode, onClick = { onModeSelected(mode) })
                }
            }
        }
    }
}

@Composable
private fun ModeCard(mode: IssueLabelFilter, onClick: () -> Unit) {
    val accent = mode.colorScheme().primary
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.10f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .background(accent, RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text(
                        mode.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                    )
                }
            }
        }
    }
}
