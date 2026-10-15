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
 * The Issue Mode gate (v0.41): shown by DiscoverNavHost instead of the
 * normal repo-first flow whenever the Issue Mode is null — a real,
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
 *
 * NESTED GitHubIssueFinderTheme(issueMode = null): added after a real,
 * reported artifact — tapping a card calls onModeSelected, which sets
 * the Issue Mode synchronously, and MainActivity's single root theme
 * call reads that same value, so EVERY currently-composed screen
 * (including this one, still visible/fading during the nav transition
 * to Top Repos) recomposes with the new mode's colors immediately.
 * Theme.kt's animateAllColors() only crossfades a handful of roles
 * (primary, background, etc.) — onSurfaceVariant and the default text
 * color aren't among them, so this screen's subheader/title visibly
 * SNAPPED from the neutral scheme's muted gray to the new mode's
 * (unanimated) tone in the instant before the screen changed — reported
 * as "it goes into a darker, more readable color, but then it just
 * changes to the next screen." The subheader text itself never changes
 * by selection; only its inherited color did, incidentally, contradicting
 * this screen's own stated intent just above ("deliberately doesn't
 * switch the app theme"). Wrapping this screen's own content in a
 * second, explicitly issueMode = null GitHubIssueFinderTheme pins
 * MaterialTheme.colorScheme to the neutral baseline for everything in
 * this subtree, regardless of what the Issue Mode becomes mid-tap —
 * restoring that stated intent rather than adding a new one.
 *
 * APP TITLE: added a plain TopAppBar (title only, no navigation icon —
 * this is the very first screen, nothing to navigate back to) so a user
 * landing here — including one who taps Reset App later and ends up
 * back here — can see which app this is, same as every other screen in
 * this flow.
 *
 * EXPANDING MODE CARDS (added after direct feedback that "good first
 * issue" wasn't self-explanatory to a user who isn't deep in GitHub/
 * OSS jargon): replaces this screen's earlier plain, whole-card-tappable
 * shape. The colored pill itself is now the ONLY thing that selects a
 * mode (ModeCard's onSelect) — tapping anywhere else on the card no
 * longer does. A separate chevron to the pill's right toggles an inline
 * description (IssueLabelFilter.description — the same wording
 * LabelInfoDialog shows, now sourced from one place so the two can't
 * drift apart) without selecting anything, so a user can read what a
 * mode means before committing to it. A plain monochrome glyph
 * (IssueLabelFilter.glyph) sits in the pill next to its name — this
 * codebase's existing plain-text-symbol convention (see e.g.
 * RepoIssuesScreen's "←"/"↗"), not an icon-library drawable, and
 * rendered in the same white as the pill's own text so it reads as
 * monochromatic rather than a colorful emoji.
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
