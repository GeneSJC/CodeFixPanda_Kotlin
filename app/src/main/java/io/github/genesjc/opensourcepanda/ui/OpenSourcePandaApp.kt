package io.github.genesjc.opensourcepanda.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.genesjc.opensourcepanda.ui.modegate.IssueModeViewModel
import io.github.genesjc.opensourcepanda.ui.navigation.DiscoverNavHost
import io.github.genesjc.opensourcepanda.ui.splash.StartupSplash
import io.github.genesjc.opensourcepanda.ui.theme.OpenSourcePandaTheme

/**
 * The root of the UI, what MainActivity hands to setContent: the whole app inside the theme of the chosen Issue Mode.
 */
@Composable
fun OpenSourcePandaApp() {
    // The Issue Mode themes the whole app, splash included, so it is observed here at the root.
    // viewModel() is Activity-scoped: DiscoverNavHost's own viewModel() call gets this same
    // instance, so the theme changes the moment the mode gate's choice lands.
    // Null (no mode chosen yet) gives the plain light/dark scheme.
    val issueModeViewModel: IssueModeViewModel = viewModel()
    val issueMode by issueModeViewModel.mode.collectAsStateWithLifecycle()
    OpenSourcePandaTheme(issueMode = issueMode) {
        StartupGate()
    }
}

/** The start splash until it finishes, then the Discover flow. */
@Composable
private fun StartupGate() {
    // Survives rotation because MainActivity is not recreated on it (android:configChanges).
    var showSplash by remember { mutableStateOf(true) }
    if (showSplash) {
        StartupSplash(onFinished = { showSplash = false })
    } else {
        DiscoverNavHost()
    }
}
