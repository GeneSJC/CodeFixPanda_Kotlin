package io.github.genesjc.githubissuefinder.ui.welcome

import io.github.genesjc.githubissuefinder.data.ApiTokenRepository
import io.github.genesjc.githubissuefinder.data.WelcomeRepository
import io.github.genesjc.githubissuefinder.data.needsWelcome
import io.github.genesjc.githubissuefinder.ui.BaseViewModel
import org.koin.core.component.inject

/**
 * The welcome screen's interface to the token and to "has the welcome been shown". Leaving the welcome
 * by any route marks it as seen, so it is not shown again at the next launch; the menu reopens it later.
 * The token text being typed stays in the composable, like in Settings.
 */
class WelcomeViewModel : BaseViewModel() {

    private val tokenRepository: ApiTokenRepository by inject()
    private val welcomeRepository: WelcomeRepository by inject()

    /** True when the app should start on the welcome: no token yet and the welcome not shown before. */
    fun needsWelcomeAtStart(): Boolean =
        needsWelcome(hasToken = !tokenRepository.isDemo.value, welcomeSeen = welcomeRepository.hasSeenWelcome)

    /**
     * Saves [token] as the user's own and marks the welcome as done. Returns false, saving nothing and
     * leaving the welcome open, when [token] is blank.
     */
    fun saveToken(token: String): Boolean {
        if (token.isBlank()) return false
        tokenRepository.setUserToken(token)
        welcomeRepository.markWelcomeSeen()
        return true
    }

    /** Marks the welcome as done without a token ("Try the demo"). */
    fun finish() {
        welcomeRepository.markWelcomeSeen()
    }
}
