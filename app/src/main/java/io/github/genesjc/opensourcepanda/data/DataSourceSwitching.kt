package io.github.genesjc.opensourcepanda.data

/**
 * The data sources that can be real or demo, chosen on every call by [useDemo] (true when no token
 * is in use), because a token can be added or removed while the app runs. The repositories above
 * them are unchanged and never learn which one answered.
 */
class SwitchedIssueSearchApi(
    private val live: IssueSearchApi,
    private val demo: IssueSearchApi,
    private val useDemo: () -> Boolean,
) : IssueSearchApi {

    private fun current() = if (useDemo()) demo else live

    override suspend fun searchIssues(query: String, perPage: Int, page: Int): IssueSearchApiResponse =
        current().searchIssues(query, perPage, page)

    override suspend fun fetchRateLimit(): RateLimitInfo? = current().fetchRateLimit()
}

class SwitchedReadmeApi(
    private val live: ReadmeApi,
    private val demo: ReadmeApi,
    private val useDemo: () -> Boolean,
) : ReadmeApi {

    override suspend fun getReadme(repoOwner: String, repoName: String): ReadmeApiResponse =
        (if (useDemo()) demo else live).getReadme(repoOwner, repoName)
}

/** Reads and writes the real cache file, or the in-memory demo cache, so demo results never reach the file. */
class SwitchedRepoIssueGroupStorage(
    private val live: RepoIssueGroupStorage,
    private val demo: RepoIssueGroupStorage,
    private val useDemo: () -> Boolean,
) : RepoIssueGroupStorage {

    private fun current() = if (useDemo()) demo else live

    override fun readAll(): Map<String, RepoIssueGroupCacheEntry> = current().readAll()

    override fun writeAll(all: Map<String, RepoIssueGroupCacheEntry>) = current().writeAll(all)

    override fun clear() = current().clear()
}
