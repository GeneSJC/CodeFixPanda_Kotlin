package io.github.genesjc.opensourcepanda.ui.discover

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.genesjc.opensourcepanda.R
import io.github.genesjc.opensourcepanda.ui.components.IndependentNotice
import io.github.genesjc.opensourcepanda.model.IssueLabelFilter
import io.github.genesjc.opensourcepanda.ui.components.VersionLabel
import io.github.genesjc.opensourcepanda.ui.theme.DrawerSectionFindIssues
import io.github.genesjc.opensourcepanda.ui.theme.DrawerSectionRepoManagement
import io.github.genesjc.opensourcepanda.ui.theme.DrawerSectionSandbox

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
    onOpenWelcome: () -> Unit,
    onResetApp: () -> Unit,
    showPrivacyOptions: Boolean,
    onPrivacyOptions: () -> Unit,
    showAdFreeOffer: Boolean,
    onAdFreeOffer: () -> Unit,
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
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            VersionLabel(label = buildLabel)
            IndependentNotice(textAlign = TextAlign.Start, modifier = Modifier.padding(top = 4.dp))
        }
        HorizontalDivider()
        DrawerMenuSection(header = stringResource(R.string.menu_section_repo_management), background = DrawerSectionRepoManagement) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_bookmarks)) },
                onClick = {
                    onDismiss()
                    onOpenBookmarks()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_hidden_issues)) },
                onClick = {
                    onDismiss()
                    onOpenHidden()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_check_new_repos)) },
                onClick = {
                    onDismiss()
                    onCheckForNewRepos()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_clear_cache)) },
                onClick = {
                    onDismiss()
                    onClearCache()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_settings)) },
                onClick = {
                    onDismiss()
                    onOpenSettings()
                },
            )
            // Reopens the welcome slideshow: how to get a token, and a place to paste it.
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_welcome)) },
                onClick = {
                    onDismiss()
                    onOpenWelcome()
                },
            )
            // A backup for the title-bar offer, for anyone who looks here: shown only while the offer can be made.
            if (showAdFreeOffer) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_hide_ads)) },
                    onClick = {
                        onDismiss()
                        onAdFreeOffer()
                    },
                )
            }
            // Only where privacy rules require a way back to the consent choices (commercial build).
            if (showPrivacyOptions) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_privacy_options)) },
                    onClick = {
                        onDismiss()
                        onPrivacyOptions()
                    },
                )
            }
            // No confirmation: nothing is destroyed. Clears the mode and returns to the gate screen.
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_reset_app)) },
                onClick = {
                    onDismiss()
                    onResetApp()
                },
            )
        }
        HorizontalDivider()
        DrawerMenuSection(header = stringResource(R.string.menu_section_find_issues), background = DrawerSectionFindIssues) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_find_good_first_issues)) },
                onClick = {
                    onDismiss()
                    onSelectMode(IssueLabelFilter.GOOD_FIRST_ISSUE)
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_find_help_wanted_issues)) },
                onClick = {
                    onDismiss()
                    onSelectMode(IssueLabelFilter.HELP_WANTED)
                },
            )
        }
        HorizontalDivider()
        DrawerMenuSection(header = stringResource(R.string.menu_section_sandbox), background = DrawerSectionSandbox) {
            // Detailed menu: one entry per animation (a dev aid). Otherwise a single "Play App Start Animation".
            if (showAllAnimations) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_play_origami)) },
                    onClick = {
                        onDismiss()
                        onReplay(AnimationReplay.ORIGAMI)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_play_shape_stack)) },
                    onClick = {
                        onDismiss()
                        onReplay(AnimationReplay.SHAPE_STACK)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_play_coin)) },
                    onClick = {
                        onDismiss()
                        onReplay(AnimationReplay.COIN)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_play_title_reveal)) },
                    onClick = {
                        onDismiss()
                        onReplay(AnimationReplay.TITLE_REVEAL)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_play_title_reveal_half_speed)) },
                    onClick = {
                        onDismiss()
                        onReplay(AnimationReplay.TITLE_REVEAL_HALF_SPEED)
                    },
                )
            } else {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_play_app_start)) },
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
