package io.github.genesjc.codefixpanda.ui.welcome

import io.github.genesjc.codefixpanda.data.ApiTokenRepository
import io.github.genesjc.codefixpanda.model.TokenSource
import io.github.genesjc.codefixpanda.ui.BaseViewModel
import kotlinx.coroutines.flow.StateFlow
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

    /** The token the user saved, or empty. The welcome offers to remove it while there is one. */
    val userToken: StateFlow<String> = tokenRepository.userToken

    /** Which token is in use: the user's, the build's, or none. The welcome explains a build token cannot be removed. */
    val tokenSource: StateFlow<TokenSource> = tokenRepository.tokenSource

    /** Removes the user's saved token from storage, falling back to the build token if there is one. */
    fun clearToken() {
        tokenRepository.setUserToken("")
    }

    /**
     * Shows the demo data for this session, even if a token is set, so "Try the demo" does what it says. The
     * token is not touched; the next launch is back to normal.
     */
    fun tryDemo() {
        tokenRepository.startDemo()
    }

    /** Saves [token] as the user's own. Returns false, saving nothing, when [token] is blank. */
    fun saveToken(token: String): Boolean {
        if (token.isBlank()) return false
        tokenRepository.setUserToken(token)
        return true
    }
}
