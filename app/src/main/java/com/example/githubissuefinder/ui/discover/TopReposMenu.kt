package com.example.githubissuefinder.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.githubissuefinder.model.IssueLabelFilter
import com.example.githubissuefinder.ui.components.VersionLabel
import com.example.githubissuefinder.ui.theme.DrawerSectionFindIssues
import com.example.githubissuefinder.ui.theme.DrawerSectionRepoManagement
import com.example.githubissuefinder.ui.theme.DrawerSectionSandbox

/**
 * The hamburger menu: the app name and build label, then three groups (Repo Management, Find
 * Issues, and the dev-only Sandbox, last). Stateless: every choice is a callback, and each item
 * dismisses the menu first.
 */
@Composable
internal fun TopReposMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    buildLabel: String,
    showAllAnimations: Boolean,
    onOpenBookmarks: () -> Unit,
    onOpenHidden: () -> Unit,
    onCheckForNewRepos: () -> Unit,
    onClearCache: () -> Unit,
    onOpenSettings: () -> Unit,
    onResetApp: () -> Unit,
    onSelectMode: (IssueLabelFilter) -> Unit,
    onReplay: (AnimationReplay) -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        // Always-visible app name and build label, so the running build can be checked against
        // the version line in chat.
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                "GitHub Issue Finder",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            VersionLabel(label = buildLabel)
        }
        HorizontalDivider()
        DrawerMenuSection(header = "Repo Management", background = DrawerSectionRepoManagement) {
            DropdownMenuItem(
                text = { Text("Bookmarks") },
                onClick = {
                    onDismiss()
                    onOpenBookmarks()
                },
            )
            DropdownMenuItem(
                text = { Text("Hidden Issues") },
                onClick = {
                    onDismiss()
                    onOpenHidden()
                },
            )
            DropdownMenuItem(
                text = { Text("Check for New Repos") },
                onClick = {
                    onDismiss()
                    onCheckForNewRepos()
                },
            )
            DropdownMenuItem(
                text = { Text("Clear Cache") },
                onClick = {
                    onDismiss()
                    onClearCache()
                },
            )
            DropdownMenuItem(
                text = { Text("Settings") },
                onClick = {
                    onDismiss()
                    onOpenSettings()
                },
            )
            // No confirmation: nothing is destroyed. Clears the mode and returns to the gate screen.
            DropdownMenuItem(
                text = { Text("Reset App") },
                onClick = {
                    onDismiss()
                    onResetApp()
                },
            )
        }
        HorizontalDivider()
        DrawerMenuSection(header = "Find Issues", background = DrawerSectionFindIssues) {
            DropdownMenuItem(
                text = { Text("Find Good First Issues") },
                onClick = {
                    onDismiss()
                    onSelectMode(IssueLabelFilter.GOOD_FIRST_ISSUE)
                },
            )
            DropdownMenuItem(
                text = { Text("Find Help Wanted Issues") },
                onClick = {
                    onDismiss()
                    onSelectMode(IssueLabelFilter.HELP_WANTED)
                },
            )
        }
        HorizontalDivider()
        DrawerMenuSection(header = "Sandbox", background = DrawerSectionSandbox) {
            // Detailed menu: one entry per animation (a dev aid). Otherwise a single "Play App Start Animation".
            if (showAllAnimations) {
                DropdownMenuItem(
                    text = { Text("Play Origami Animation") },
                    onClick = {
                        onDismiss()
                        onReplay(AnimationReplay.ORIGAMI)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Play Shape Stack Animation") },
                    onClick = {
                        onDismiss()
                        onReplay(AnimationReplay.SHAPE_STACK)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Play Coin Animation") },
                    onClick = {
                        onDismiss()
                        onReplay(AnimationReplay.COIN)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Play Title Reveal Animation") },
                    onClick = {
                        onDismiss()
                        onReplay(AnimationReplay.TITLE_REVEAL)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Play Title Reveal Animation (0.5x Speed)") },
                    onClick = {
                        onDismiss()
                        onReplay(AnimationReplay.TITLE_REVEAL_HALF_SPEED)
                    },
                )
            } else {
                DropdownMenuItem(
                    text = { Text("Play App Start Animation") },
                    onClick = {
                        onDismiss()
                        onReplay(AnimationReplay.APP_START)
                    },
                )
            }
        }
    }
}

/** A pastel-tinted, headed group of menu items. Callers put a divider between sections. */
@Composable
private fun DrawerMenuSection(
    header: String,
    background: Color,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.background(background)) {
        Text(
            header,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 2.dp),
        )
        content()
    }
}
