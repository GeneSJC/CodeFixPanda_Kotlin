package io.github.genesjc.opensourcepanda

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.genesjc.opensourcepanda.ui.ads.AdsViewModel
import io.github.genesjc.opensourcepanda.ui.modegate.IssueModeViewModel
import io.github.genesjc.opensourcepanda.ui.navigation.DiscoverNavHost
import io.github.genesjc.opensourcepanda.ui.splash.AppStartSplashScreen
import io.github.genesjc.opensourcepanda.ui.splash.StartupSplashViewModel
import io.github.genesjc.opensourcepanda.ui.theme.OpenSourcePandaTheme

/**
 * The manifest's android:configChanges stops the system from recreating this Activity on rotation.
 * Recreation reset `showSplash` (replaying the start animation on a running app) and was the
 * suspected cause of a white screen after rotating to landscape. With it, Compose just recomposes
 * in place, which is how TopReposScreen's landscape detection expects to work.
 */
class MainActivity : ComponentActivity() {

    private val adsViewModel: AdsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Consent, then advertising, in the commercial build; nothing in the open build.
        adsViewModel.start(this)
        setContent {
            // The Issue Mode themes the whole app, splash included, so it is observed here at the root.
            // viewModel() is Activity-scoped: DiscoverNavHost's own viewModel() call gets this same
            // instance, so the theme changes the moment the mode gate's choice lands.
            // Null (no mode chosen yet) gives the plain light/dark scheme.
            val issueModeViewModel: IssueModeViewModel = viewModel()
            val issueMode by issueModeViewModel.mode.collectAsStateWithLifecycle()
            OpenSourcePandaTheme(issueMode = issueMode) {
                var showSplash by remember { mutableStateOf(true) }
                if (showSplash) {
                    // Full animation on the first launch, a short version that
                    // starts at the settled title on every launch after — decided once
                    // per launch, see StartupSplashViewModel.
                    val splashViewModel: StartupSplashViewModel = viewModel()
                    val onFinished = {
                        splashViewModel.onSplashFinished()
                        showSplash = false
                    }
                    // AppStartSplashScreen picks the splash, so the drawer's "Play App Start Animation"
                    // replay (AnimationReplay.kt) cannot drift from what a user sees at launch.
                    AppStartSplashScreen(
                        onFinished = onFinished,
                        animation = splashViewModel.splashAnimation,
                        playFullAnimation = splashViewModel.playFullAnimation,
                    )
                } else {
                    // The Discover flow.
                    DiscoverNavHost()
                }
            }
        }
    }
}
