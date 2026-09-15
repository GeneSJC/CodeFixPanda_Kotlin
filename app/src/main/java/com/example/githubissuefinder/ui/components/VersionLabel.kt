package com.example.githubissuefinder.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.githubissuefinder.BuildConfig

/**
 * Small, unobtrusive Commit Milestone version stamp, shown beneath the
 * title in each top-level screen's TopAppBar. Single source of truth is
 * BuildConfig.CM_VERSION (app/build.gradle.kts) — bumping that one line
 * is the ONLY per-CM maintenance this requires; no other file needs to
 * change to keep this accurate.
 *
 * Deliberately separate from the app's real Android versionName/
 * versionCode, which track actual app releases. CM_VERSION exists purely
 * so it's possible to visually confirm, on-device, which Commit
 * Milestone's changes are actually running — instead of relying on file
 * diffs alone.
 */
@Composable
fun VersionLabel(modifier: Modifier = Modifier) {
    Text(
        BuildConfig.CM_VERSION,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
