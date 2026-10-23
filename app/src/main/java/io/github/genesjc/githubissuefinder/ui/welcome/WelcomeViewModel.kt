package io.github.genesjc.githubissuefinder.ui.welcome

import io.github.genesjc.githubissuefinder.data.ApiTokenRepository
import io.github.genesjc.githubissuefinder.ui.BaseViewModel
import org.koin.core.component.inject

/**
 * The welcome screen's interface to the token. The app starts on the welcome whenever there is no token,
 * on every launch: the demo is a way to look around for one session, not a way to skip getting a token.
 * The token text being typed stays in the composable, like in Settings.
 */
class WelcomeViewModel : BaseViewModel() {

    private val tokenRepository: ApiTokenRepository by inject()

    /** True when the app should start on the welcome: no token is set, from the user or from the build. */
    fun needsWelcomeAtStart(): Boolean = tokenRepository.isDemo.value

    /** The token in effect (the user's, else the build's), or empty. The welcome starts its field with it when reopened. */
    val currentToken: String
        get() = tokenRepository.effectiveToken.value

    /** Saves [token] as the user's own. Returns false, saving nothing, when [token] is blank. */
    fun saveToken(token: String): Boolean {
        if (token.isBlank()) return false
        tokenRepository.setUserToken(token)
        return true
    }
}
