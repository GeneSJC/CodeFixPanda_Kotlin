package io.github.genesjc.opensourcepanda.ui.ads

import android.app.Activity
import io.github.genesjc.opensourcepanda.ads.AdsController
import io.github.genesjc.opensourcepanda.ui.BaseViewModel
import org.koin.core.component.inject

/**
 * The app shell's interface to advertising: starting it once at launch. MainActivity calls it, so the
 * Activity talks to a ViewModel like every other View does. The Activity is handed over for the call
 * only (the consent form needs one) and is never kept, so nothing leaks across a configuration change.
 * The controller is a plain member that Koin supplies; the build flavor decides which one.
 */
class AdsViewModel : BaseViewModel() {

    private val adsController: AdsController by inject()

    /** Runs the consent flow and starts advertising. Does nothing in the open build. */
    fun start(activity: Activity) {
        adsController.start(activity)
    }
}
