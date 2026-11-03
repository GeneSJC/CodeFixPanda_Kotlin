package io.github.genesjc.opensourcepanda.ui.toprepos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.genesjc.opensourcepanda.R
import io.github.genesjc.opensourcepanda.model.IssueLabelFilter
import io.github.genesjc.opensourcepanda.ui.components.BrandTitleBand
import io.github.genesjc.opensourcepanda.ui.components.IndependentNotice
import io.github.genesjc.opensourcepanda.ui.components.VersionLabel
import io.github.genesjc.opensourcepanda.ui.theme.DrawerSectionFindIssues
import io.github.genesjc.opensourcepanda.ui.theme.DrawerSectionRepoManagement
import io.github.genesjc.opensourcepanda.ui.theme.DrawerSectionSandbox
import kotlinx.coroutines.launch

private const val DRAWER_WIDTH_FRACTION = 0.8f
private val DRAWER_MAX_WIDTH: Dp = 360.dp

/**
 * The navigation drawer around the Top Repos screen: the brand band (glyph and panda), the build label, then three
 * groups (Repo Management, Find Issues, and the dev-only Sandbox, last). Stateless apart from the
 * [drawerState] the caller owns: every choice is a callback, and each item closes the drawer
 * first. Back closes it too (Material's modal drawer handles that itself).
 */
@Composable
internal fun TopReposDrawer(
    drawerState: DrawerState,
    buildLabel: String,
    currentMode: IssueLabelFilter,
    showAllAnimations: Boolean,
    onOpenBookmarks: () -> Unit,
    onOpenHidden: () -> Unit,
    onCheckForNewRepos: () -> Unit,
    onClearCacheRequested: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWelcome: () -> Unit,
    onResetApp: () -> Unit,
    onSelectMode: (IssueLabelFilter) -> Unit,
    onAboutIssueTypes: () -> Unit,
    onReplay: (AnimationReplay) -> Unit,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    // 80% of the screen, so the app shows through beside it; capped so wide screens do not get a huge sheet.
    val drawerWidth = minOf((LocalConfiguration.current.screenWidthDp * DRAWER_WIDTH_FRACTION).dp, DRAWER_MAX_WIDTH)

    // One item: closes the drawer, then runs the action.
    @Composable
    fun Item(label: String, selected: Boolean = false, action: () -> Unit) {
        NavigationDrawerItem(
            label = { Text(label) },
            selected = selected,
            onClick = {
                scope.launch { drawerState.close() }
                action()
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            // No window insets on the sheet: the brand band reaches under the status bar itself.
            ModalDrawerSheet(modifier = Modifier.width(drawerWidth), windowInsets = WindowInsets(0)) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding()) {
                    BrandTitleBand()
                    // Always-visible build label, so the running build can be matched to a Gradle log or a commit.
                    Column(modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp)) {
                        VersionLabel(label = buildLabel)
                        IndependentNotice(textAlign = TextAlign.Start, modifier = Modifier.padding(top = 4.dp))
                    }

                    HorizontalDivider()
                    DrawerSection(header = stringResource(R.string.menu_section_repo_management), background = DrawerSectionRepoManagement) {
                        Item(stringResource(R.string.menu_bookmarks)) { onOpenBookmarks() }
                        Item(stringResource(R.string.menu_hidden_issues)) { onOpenHidden() }
                        Item(stringResource(R.string.menu_check_new_repos)) { onCheckForNewRepos() }
                        Item(stringResource(R.string.action_clear_cache)) { onClearCacheRequested() }
                        Item(stringResource(R.string.menu_settings)) { onOpenSettings() }
                        // Reopens the welcome slideshow: how to get a token, and a place to paste it.
                        Item(stringResource(R.string.menu_welcome)) { onOpenWelcome() }
                        // No confirmation: nothing is destroyed. Clears the mode and returns to the gate screen.
                        Item(stringResource(R.string.menu_reset_app)) { onResetApp() }
                    }

                    HorizontalDivider()
                    DrawerSection(header = stringResource(R.string.menu_section_find_issues), background = DrawerSectionFindIssues) {
                        Item(
                            stringResource(R.string.menu_find_good_first_issues),
                            selected = currentMode == IssueLabelFilter.GOOD_FIRST_ISSUE,
                        ) { onSelectMode(IssueLabelFilter.GOOD_FIRST_ISSUE) }
                        Item(
                            stringResource(R.string.menu_find_help_wanted_issues),
                            selected = currentMode == IssueLabelFilter.HELP_WANTED,
                        ) { onSelectMode(IssueLabelFilter.HELP_WANTED) }
                        Item(stringResource(R.string.menu_about_issue_types)) { onAboutIssueTypes() }
                    }

                    HorizontalDivider()
                    DrawerSection(header = stringResource(R.string.menu_section_sandbox), background = DrawerSectionSandbox) {
                        // Detailed menu: one entry per animation (a dev aid). Otherwise a single "Play App Start Animation".
                        if (showAllAnimations) {
                            Item(stringResource(R.string.menu_play_origami)) { onReplay(AnimationReplay.ORIGAMI) }
                            Item(stringResource(R.string.menu_play_shape_stack)) { onReplay(AnimationReplay.SHAPE_STACK) }
                            Item(stringResource(R.string.menu_play_coin)) { onReplay(AnimationReplay.COIN) }
                            Item(stringResource(R.string.menu_play_title_reveal)) { onReplay(AnimationReplay.TITLE_REVEAL) }
                            Item(stringResource(R.string.menu_play_title_reveal_half_speed)) {
                                onReplay(AnimationReplay.TITLE_REVEAL_HALF_SPEED)
                            }
                        } else {
                            Item(stringResource(R.string.menu_play_app_start)) { onReplay(AnimationReplay.APP_START) }
                        }
                    }
                }
            }
        },
        content = content,
    )
}

/** A pastel-tinted, headed group of drawer items. Callers put a divider between sections. */
@Composable
private fun DrawerSection(
    header: String,
    background: Color,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.background(background).padding(bottom = 8.dp)) {
        Text(
            header,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 28.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        )
        content()
    }
}
