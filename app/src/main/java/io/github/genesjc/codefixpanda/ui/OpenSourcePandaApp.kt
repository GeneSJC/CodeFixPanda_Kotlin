package io.github.genesjc.codefixpanda.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.genesjc.codefixpanda.ui.modegate.IssueModeViewModel
import io.github.genesjc.codefixpanda.ui.navigation.DiscoverNavHost
import io.github.genesjc.codefixpanda.ui.splash.StartupSplash
import io.github.genesjc.codefixpanda.ui.theme.OpenSourcePandaTheme

/**
 * The root of the UI, what MainActivity hands to setContent: the whole app inside the theme of the chosen
 * Issue Mode, showing the start splash first and the app once it has finished.
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
        // For this launch only (StartupSplashViewModel decides which splash plays, per device).
        // Survives rotation because MainActivity is not recreated on it (android:configChanges).
        var hasShownSplash by remember { mutableStateOf(false) }
        if (hasShownSplash) {
            DiscoverNavHost()
            return@OpenSourcePandaTheme
        }

        StartupSplash(onFinished = { hasShownSplash = true })
    }
}
