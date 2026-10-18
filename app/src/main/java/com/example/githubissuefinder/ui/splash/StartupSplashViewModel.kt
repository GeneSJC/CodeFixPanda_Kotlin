package com.example.githubissuefinder.ui.splash

import com.example.githubissuefinder.data.StartupSplashRepository
import com.example.githubissuefinder.ui.BaseViewModel
import org.koin.core.component.inject

/**
 * Decides once per launch whether the start splash is the full title-reveal or the short settled-title version,
 * depending on whether this device has already seen the full one finish. [playFullAnimation] is a
 * val computed at construction, not a live read: the flag is written when the animation ends, and
 * re-reading it would flip the answer mid-animation. Activity-scoped; the repository is a plain
 * member that Koin supplies.
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
