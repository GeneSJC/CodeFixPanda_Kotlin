package io.github.genesjc.opensourcepanda.data

import io.github.genesjc.opensourcepanda.domain.REPO_ISSUES_MAX_PER_PAGE
import io.github.genesjc.opensourcepanda.domain.TOP_REPOS_PER_PAGE
import io.github.genesjc.opensourcepanda.model.IssueLabelFilter
import io.github.genesjc.opensourcepanda.model.LanguageFilter
import io.github.genesjc.opensourcepanda.model.RateLimitInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The Search API query for Top Repos: open issues carrying the mode's label, optionally in one language. */
fun topReposQuery(mode: IssueLabelFilter, language: LanguageFilter): String = buildString {
    append("label:\"${mode.queryValue}\"")
    append(" state:open")
    language.queryValue?.let { append(" language:$it") }
}

/** The Search API query for one repo's own issues screen: that repo's open issues carrying the mode's label. */
fun repoIssuesQuery(repoFullName: String, mode: IssueLabelFilter): String =
    "repo:$repoFullName label:\"${mode.queryValue}\" state:open"

/**
 * The single place the app searches GitHub for issues: Top Repos' list (with Load More and Check
 * for New Repos) and a single repo's refresh. It turns an HTTP response into an [IssueSearchResult]
 * callers can switch on, and keeps the rules in one place. Which data source answers (real, or the
 * demo one when there is no token) is decided beneath it, in the [IssueSearchApi] it is given. The
 * rate-limit chip's numbers live here in [rateLimit], updated from the headers of every successful
 * search; a failed response is only inspected to tell a rate-limit 403 from other errors. They
 * describe the credential's shared Search API quota, not one search, which is why a repository
 * holds them and not a screen.
 */
class IssueSearchRepository(
    private val api: IssueSearchApi,
) {

    private val _rateLimit = MutableStateFlow<RateLimitInfo?>(null)

    /** The Search API rate-limit numbers for the chip, or null until a search (or the free lookup) has supplied them. */
    val rateLimit: StateFlow<RateLimitInfo?> = _rateLimit.asStateFlow()

    /** Forgets the numbers, for when the token changes: the old token's quota doesn't describe the new one. */
    fun clearRateLimit() {
        _rateLimit.value = null
    }

    /** One page of Top Repos' list for [mode] and [language]. [page] is 1-indexed. */
    suspend fun searchTopRepos(mode: IssueLabelFilter, language: LanguageFilter, page: Int): IssueSearchResult =
        search(topReposQuery(mode, language), TOP_REPOS_PER_PAGE, page, language)

    /** Every open matching issue of one repo (up to [REPO_ISSUES_MAX_PER_PAGE]), for its "Refresh this repo". */
    suspend fun searchRepoIssues(repoFullName: String, mode: IssueLabelFilter, language: LanguageFilter): IssueSearchResult =
        search(repoIssuesQuery(repoFullName, mode), REPO_ISSUES_MAX_PER_PAGE, 1, language)

    /**
     * Fills in the rate-limit numbers when none are known yet (a list
     * loaded from the cache never made a search that could supply them).
     * Free: it uses GitHub's /rate_limit endpoint, which doesn't count
     * against any limit. A failure is ignored on purpose — the chip just
     * stays blank, and the next search fills it in.
     */
    suspend fun loadRateLimitIfUnknown() {
        if (_rateLimit.value != null) return
        val info = api.fetchRateLimit() ?: return
        // Check again: this call waited for the network, and a search that finished meanwhile may have set fresher
        // numbers, which this older lookup must not overwrite.
        if (_rateLimit.value == null) _rateLimit.value = info
    }

    private suspend fun search(query: String, perPage: Int, page: Int, language: LanguageFilter): IssueSearchResult {
        return try {
            val response = api.searchIssues(query, perPage, page)
            val body = response.body
            if (!response.isSuccessful || body == null) {
                return IssueSearchResult.HttpError(
                    code = response.code,
                    rateLimited = response.code == 403 && response.rateLimit?.remaining == 0,
                )
            }

            _rateLimit.value = response.rateLimit
            IssueSearchResult.Success(
                IssueSearchPage(
                    issues = body.items.map { it.toIssue(languageFilter = language.queryValue, origin = response.origin) },
                    totalCount = body.totalCount,
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            IssueSearchResult.Failed(e.message)
        }
    }
}
