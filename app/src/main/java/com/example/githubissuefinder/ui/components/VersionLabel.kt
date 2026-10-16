package com.example.githubissuefinder.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The on-device build label — "Build A.B" (Project Conventions doc,
 * section 6). The label is passed in by the screen, from its ViewModel's
 * buildLabel; its single source of truth is BuildConfig.BUILD_LABEL
 * (app/build.gradle.kts's buildLabelA/buildLabelB), read only by AppConfigRepository — bumping those two
 * vals is the only maintenance required to keep this accurate; no other
 * file needs to change.
 *
 * Deliberately separate from the app's real Android versionName/
 * versionCode, which track actual app releases, and NOT a semantic or
 * release version — purely a build identifier, so a device's screen can
 * be checked against a chat message's own closing "Version: A.B" line.
 *
 * Shown unconditionally in TopReposScreen's drawer header, under the
 * app name — not gated behind BuildConfig.SHOW_DETAILED_DRAWER_MENU
 * the way this project's old CM version stamp was, since the whole
 * point is knowing which build is on-device at a glance, for anyone,
 * not just during active dev iteration. Previously defined but never
 * actually wired into any screen (a real gap — this component existed
 * but was dead code until this).
 */
@Composable
fun VersionLabel(label: String, modifier: Modifier = Modifier) {
    Text(
        "Build $label",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
