package io.github.genesjc.opensourcepanda.data

import io.github.genesjc.opensourcepanda.network.GitHubApiService
import io.github.genesjc.opensourcepanda.network.decodeGitHubContentsBase64
import io.github.genesjc.opensourcepanda.network.dto.ReadmeDto
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
 * GitHub's Contents API (GET /repos/{owner}/{repo}/readme), run off the main thread. A different
 * API and quota from search, so README fetches are deliberately not counted by
 * IssueSearchRepository.rateLimit.
 */
class GitHubReadmeApi(private val service: GitHubApiService) : ReadmeApi {
    override suspend fun getReadme(repoOwner: String, repoName: String): ReadmeApiResponse =
        withContext(Dispatchers.IO) {
            val response = service.getReadme(repoOwner, repoName)
            ReadmeApiResponse(response.code(), response.isSuccessful, response.body())
        }
}

/**
 * The single place the app fetches a repo's README. It turns an HTTP response into a
 * [ReadmeResult] callers can switch on, and keeps the rules in one place: a 404 is "no README" and
 * not an error, and an undecodable body is its own outcome. Whether the real or the demo API answers
 * is decided beneath it, in the [ReadmeApi] it is given.
 * [decode] is a parameter so a test can control it; the real base64 decoding
 * uses android.util.Base64, which plain JVM tests do not have.
 */
class ReadmeRepository(
    private val api: ReadmeApi,
    private val decode: (content: String?, encoding: String?) -> String? = ::decodeGitHubContentsBase64,
) {

    suspend fun fetch(repoOwner: String, repoName: String): ReadmeResult {
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
}
