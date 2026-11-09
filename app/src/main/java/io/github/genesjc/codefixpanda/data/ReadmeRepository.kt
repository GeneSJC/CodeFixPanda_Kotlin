package io.github.genesjc.codefixpanda.data

import io.github.genesjc.codefixpanda.data.network.decodeGitHubContentsBase64
import kotlinx.coroutines.CancellationException

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
            val response = api.fetchReadme(repoOwner, repoName)
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
