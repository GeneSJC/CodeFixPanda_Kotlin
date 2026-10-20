package com.example.githubissuefinder.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.githubissuefinder.R

/**
 * The on-device build label, "Build A.B", passed in from the ViewModel's buildLabel. Its single
 * source is buildLabelA/buildLabelB in app/build.gradle.kts, so bumping those is the only
 * maintenance. It is a build identifier, not the app's versionName or versionCode, so a device
 * screen can be checked against a chat message's closing "Version: A.B" line. Shown in the Top Repos
 * menu header at all times, so anyone can see which build is on the device.
 */
@Composable
fun VersionLabel(label: String, modifier: Modifier = Modifier) {
    Text(
        stringResource(R.string.version_label, label),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
