package io.github.genesjc.opensourcepanda.ui.settings

import io.github.genesjc.opensourcepanda.data.ApiTokenRepository
import io.github.genesjc.opensourcepanda.data.TokenSource
import io.github.genesjc.opensourcepanda.ui.BaseViewModel
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.inject

/**
 * The Settings screen's interface to the GitHub token: what is saved, which source is in effect, and
 * saving or clearing the user's own. Persistence and the "user token beats build token" rule are in
 * ApiTokenRepository. It deliberately does not hold the text being typed: that draft is short-lived
 * presentation state and stays in the composable.
 */
class ApiTokenViewModel : BaseViewModel() {

    private val repository: ApiTokenRepository by inject()

    /** The token the user saved, or empty if none. */
    val userToken: StateFlow<String> = repository.userToken

    /** The token in effect: the user's if they saved one, else the build-time one, else empty. Shown masked in Settings. */
    val currentToken: StateFlow<String> = repository.effectiveToken

    /** Which token is actually in use right now: the user's, the build-time one, or none. */
    val tokenSource: StateFlow<TokenSource> = repository.tokenSource

    /** Saves [token] as the user's own. Whitespace is trimmed; a blank value clears it. */
    fun saveToken(token: String) {
        repository.setUserToken(token)
    }

    /** Removes the user's token, falling back to the build-time one if there is one. */
    fun clearToken() {
        repository.setUserToken("")
    }
}
