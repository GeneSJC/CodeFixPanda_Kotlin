package com.example.githubissuefinder.ui.splash

import com.example.githubissuefinder.data.StartupSplashRepository
import com.example.githubissuefinder.ui.BaseViewModel
import org.koin.core.component.inject

/**
 * Decides, once per launch, whether the start splash is the full
 * title-reveal animation (this device has never seen it finish) or the
 * short fade-in of the app name (it has).
 *
 * [playFullAnimation] is a plain val computed at construction, not a
 * live read: the repository's flag is written when the animation ends,
 * and re-reading it afterward would flip the answer to "short" while
 * the full animation is still on screen (or on a recomposition right
 * after it). Deciding once and holding the answer for this ViewModel's
 * lifetime keeps one launch's splash consistent.
 *
 * Scoped to the Activity (obtained with viewModel() from MainActivity's
 * root composition). The repository is a plain member that Koin supplies;
 * a test registers one over a fake storage.
 */
class StartupSplashViewModel : BaseViewModel() {

    private val repository: StartupSplashRepository by inject()

    val playFullAnimation: Boolean = !repository.hasSeenFullAnimation

    /**
     * Called when whichever splash played has finished. Recording that
     * the full animation has been seen only matters after the full one
     * ran; calling it after the short one is harmless (the flag is
     * already set). A launch that is killed mid-animation never reaches
     * this, so the full animation plays again next time.
     */
    fun onSplashFinished() {
        repository.markFullAnimationSeen()
    }
}
