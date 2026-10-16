package com.example.githubissuefinder.data

import com.example.githubissuefinder.network.GitHubApiClient
import com.example.githubissuefinder.network.decodeGitHubContentsBase64
import com.example.githubissuefinder.network.dto.ReadmeDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Every way fetching a repo's README can end, so callers react to an
 * outcome instead of poking at an HTTP response themselves.
 */
sealed interface ReadmeResult {
    /** The README's decoded markdown text. */
    data class Found(val markdown: String) : ReadmeResult

    /** The repo has no README at the root of its default branch (a real, expected 404, not an error). */
    data object NotFound : ReadmeResult

    /** No GitHub token is in use, so nothing was requested. */
    data object NoToken : ReadmeResult

    /** GitHub answered, but with an error (anything other than success or the 404 above). */
    data class HttpError(val code: Int) : ReadmeResult

    /** GitHub answered with a README the app couldn't decode. */
    data object Unreadable : ReadmeResult

    /** The request itself failed (no network, a timeout, ...): worth trying again later. */
    data class Failed(val message: String?) : ReadmeResult
}

/** What came back from the README endpoint: the HTTP status, and the body if there was one. */
data class ReadmeApiResponse(val code: Int, val isSuccessful: Boolean, val body: ReadmeDto?)

/**
 * The one call this repository makes, split out so the repository's own
 * rules can be unit-tested against a plain fake with no network.
 */
fun interface ReadmeApi {
    suspend fun getReadme(repoOwner: String, repoName: String): ReadmeApiResponse
}

/**
 * GitHub's Contents API (GET /repos/{owner}/{repo}/readme), run off the
 * main thread. A different API and quota (5,000/hour authenticated)
 * from the Search API everything else uses, which is why README
 * fetches are deliberately NOT counted by RateLimitState.
 *
 * This is the only place in the app that talks to GitHub for README
 * text: the README screen and the repo-card previews both go through
 * ReadmeRepository, which uses this.
 */
class GitHubReadmeApi : ReadmeApi {
    override suspend fun getReadme(repoOwner: String, repoName: String): ReadmeApiResponse =
        withContext(Dispatchers.IO) {
            val response = GitHubApiClient.api.getReadme(repoOwner, repoName)
            ReadmeApiResponse(response.code(), response.isSuccessful, response.body())
        }
}

/**
 * The single place the app fetches a repo's README, and the first piece
 * of the data layer between ViewModels and the network (Group B of the
 * MVVM sweep). Before this, RepoReadmeViewModel and the README snippet
 * source each made the request and decoded the response themselves.
 *
 * Why a repository: it turns "an HTTP response" into a [ReadmeResult]
 * the callers can simply switch on, keeps the rules in one place (no
 * token means no request; a 404 is "no README", not an error; a body
 * that can't be decoded is its own outcome), and lets those rules be
 * unit-tested without a network.
 *
 * [hasToken] and [decode] are parameters so a test can control them:
 * the real base64 decoding uses android.util.Base64, which isn't
 * available in plain JVM unit tests.
 */
class ReadmeRepository(
    private val api: ReadmeApi,
    private val hasToken: () -> Boolean = { ApiTokenRepository.instance.effectiveToken.value.isNotBlank() },
    private val decode: (content: String?, encoding: String?) -> String? = ::decodeGitHubContentsBase64,
) {

    suspend fun fetch(repoOwner: String, repoName: String): ReadmeResult {
        if (!hasToken()) return ReadmeResult.NoToken
        return try {
            val response = api.getReadme(repoOwner, repoName)
            if (response.code == 404) return ReadmeResult.NotFound
            val body = response.body
            if (!response.isSuccessful || body == null) return ReadmeResult.HttpError(response.code)
            val markdown = decode(body.content, body.encoding) ?: return ReadmeResult.Unreadable
            ReadmeResult.Found(markdown)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ReadmeResult.Failed(e.message)
        }
    }

    companion object {
        /** The app-wide instance. Needs no Context, so no init call is required. */
        val instance: ReadmeRepository by lazy { ReadmeRepository(GitHubReadmeApi()) }
    }
}
