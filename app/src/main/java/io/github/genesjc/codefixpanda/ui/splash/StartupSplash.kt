package io.github.genesjc.codefixpanda.ui.splash

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * The splash shown at launch: the full animation on the first launch, a short version that starts at the
 * settled title on every launch after, decided once per launch (see StartupSplashViewModel).
 * [onFinished] runs when the splash is done, after the ViewModel has recorded that it was seen.
 */
@Composable
fun StartupSplash(onFinished: () -> Unit) {
    val splashViewModel: StartupSplashViewModel = viewModel()
    // AppStartSplashScreen picks the splash, so the drawer's "Play App Start Animation"
    // replay (AnimationReplay.kt) cannot drift from what a user sees at launch.
    AppStartSplashScreen(
        onFinished = {
            splashViewModel.onSplashFinished()
            onFinished()
        },
        animation = splashViewModel.splashAnimation,
        playFullAnimation = splashViewModel.playFullAnimation,
    )
}
