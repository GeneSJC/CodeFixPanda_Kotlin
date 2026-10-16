package com.example.githubissuefinder.data

/**
 * Why a search-backed request did not produce what it was asked for,
 * reported as a fact. The ViewModel layer turns it into the wording the
 * user reads (the Top Repos list and the repo-issues refresh each word it
 * differently); the services that hold the data only say what happened.
 */
sealed interface SearchFailure {
    /** GitHub answered with an error status. */
    data class Http(val code: Int) : SearchFailure

    /** The request failed before GitHub answered (no network, a timeout, an unreadable response). */
    data class Failed(val message: String?) : SearchFailure
}
