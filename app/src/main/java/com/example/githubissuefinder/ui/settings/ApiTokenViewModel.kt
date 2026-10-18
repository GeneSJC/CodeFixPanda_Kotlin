package com.example.githubissuefinder.ui.settings

import com.example.githubissuefinder.data.ApiTokenRepository
import com.example.githubissuefinder.data.TokenSource
import com.example.githubissuefinder.ui.BaseViewModel
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.inject

/**
 * The Settings screen's interface to the GitHub token: what is saved,
 * which source is currently in effect, and saving or clearing the
 * user's own. Persistence and the "user token beats build token" rule
 * live in ApiTokenRepository, not here.
 *
 * Deliberately NOT holding the text the user is typing: that draft is
 * purely presentational, short-lived state (it only matters until Save
 * is tapped), so it stays in the composable — a ViewModel is for state
 * that is persisted, shared, or the result of a decision.
 *
 * The repository is a plain member that Koin supplies; a test registers
 * one over a fake storage.
 */
class ApiTokenViewModel : BaseViewModel() {

    private val repository: ApiTokenRepository by inject()

    /** The token the user saved, or empty if none. */
    val userToken: StateFlow<String> = repository.userToken

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
