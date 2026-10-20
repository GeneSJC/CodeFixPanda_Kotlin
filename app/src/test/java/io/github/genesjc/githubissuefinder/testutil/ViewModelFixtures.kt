package io.github.genesjc.githubissuefinder.testutil

import io.github.genesjc.githubissuefinder.ads.AdsController
import io.github.genesjc.githubissuefinder.data.ApiTokenRepository
import io.github.genesjc.githubissuefinder.data.ApiTokenStorage
import io.github.genesjc.githubissuefinder.data.AppConfig
import io.github.genesjc.githubissuefinder.data.BookmarksRepository
import io.github.genesjc.githubissuefinder.data.BookmarksStorage
import io.github.genesjc.githubissuefinder.data.HiddenIssuesRepository
import io.github.genesjc.githubissuefinder.data.HiddenIssuesStorage
import io.github.genesjc.githubissuefinder.data.IssueModeRepository
import io.github.genesjc.githubissuefinder.data.IssueModeStorage
import io.github.genesjc.githubissuefinder.data.IssueSearchApi
import io.github.genesjc.githubissuefinder.data.IssueSearchApiResponse
import io.github.genesjc.githubissuefinder.data.IssueSearchRepository
import io.github.genesjc.githubissuefinder.data.RateLimitInfo
import io.github.genesjc.githubissuefinder.data.ReadmeFetch
import io.github.genesjc.githubissuefinder.data.ReadmeSnippetRepository
import io.github.genesjc.githubissuefinder.data.ReadmeSource
import io.github.genesjc.githubissuefinder.data.RepoIssueGroupCacheEntry
import io.github.genesjc.githubissuefinder.data.RepoIssueGroupRepository
import io.github.genesjc.githubissuefinder.data.RepoIssueGroupStorage
import io.github.genesjc.githubissuefinder.data.RepoIssuesStateHolder
import io.github.genesjc.githubissuefinder.data.TopReposStateHolder
import io.github.genesjc.githubissuefinder.data.toIssue
import io.github.genesjc.githubissuefinder.model.HiddenIssue
import io.github.genesjc.githubissuefinder.model.Issue
import io.github.genesjc.githubissuefinder.model.IssueLabelFilter
import io.github.genesjc.githubissuefinder.network.dto.IssueDto
import io.github.genesjc.githubissuefinder.network.dto.IssueSearchResponseDto
import io.github.genesjc.githubissuefinder.ui.StringResolver
import io.github.genesjc.githubissuefinder.ui.discover.RepoIssuesViewModel
import io.github.genesjc.githubissuefinder.ui.discover.TopReposViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * In-memory stand-ins for everything a ViewModel talks to, so ViewModel
 * tests run on the plain JVM with no Android and no network. Each one
 * is a real repository over a fake storage or API — the repository
 * rules are exercised for real, only the outermost edge is faked.
 */

class FakeIssueModeStorage(var stored: String? = null) : IssueModeStorage {
    override fun load(): String? = stored
    override fun save(name: String?) {
        stored = name
    }
}

class FakeApiTokenStorage(var stored: String? = null) : ApiTokenStorage {
    override fun load(): String? = stored
    override fun save(token: String?) {
        stored = token
    }
}

class FakeHiddenIssuesStorage(var stored: List<HiddenIssue> = emptyList()) : HiddenIssuesStorage {
    override fun load(): List<HiddenIssue> = stored
    override fun save(issues: List<HiddenIssue>) {
        stored = issues
    }
}

class FakeBookmarksStorage(var stored: List<Issue> = emptyList()) : BookmarksStorage {
    override fun load(): List<Issue> = stored
    override fun save(issues: List<Issue>) {
        stored = issues
    }
}

class FakeRepoIssueGroupStorage(var data: Map<String, RepoIssueGroupCacheEntry> = emptyMap()) : RepoIssueGroupStorage {
    override fun readAll(): Map<String, RepoIssueGroupCacheEntry> = data
    override fun writeAll(all: Map<String, RepoIssueGroupCacheEntry>) {
        data = all
    }
    override fun clear() {
        data = emptyMap()
    }
}

/** A search API whose answer is whatever [handler] returns for each request; every request is recorded. */
class FakeIssueSearchApi(
    var handler: (query: String, perPage: Int, page: Int) -> IssueSearchApiResponse = { _, _, _ -> error("no search expected") },
    var rateLimit: RateLimitInfo? = null,
) : IssueSearchApi {
    val calls = mutableListOf<Triple<String, Int, Int>>()
    var rateLimitCalls = 0

    override suspend fun searchIssues(query: String, perPage: Int, page: Int): IssueSearchApiResponse {
        calls += Triple(query, perPage, page)
        return handler(query, perPage, page)
    }

    override suspend fun fetchRateLimit(): RateLimitInfo? {
        rateLimitCalls++
        return rateLimit
    }
}

class FakeReadmeSource(var result: ReadmeFetch = ReadmeFetch.Resolved(null)) : ReadmeSource {
    override suspend fun fetch(repoOwner: String, repoName: String): ReadmeFetch = result
}

/** Everything a Top Repos or repo-issues ViewModel (or a state holder) is built from, wired over fakes, with the pieces a test needs to poke at. */
class ViewModelFixtures(
    mode: IssueLabelFilter? = IssueLabelFilter.GOOD_FIRST_ISSUE,
    buildToken: String = "build-token",
    val api: FakeIssueSearchApi = FakeIssueSearchApi(),
    val cacheStorage: FakeRepoIssueGroupStorage = FakeRepoIssueGroupStorage(),
    val readmeSource: FakeReadmeSource = FakeReadmeSource(),
) {
    val modeStorage = FakeIssueModeStorage(mode?.name)
    val modeRepository = IssueModeRepository(modeStorage)
    val tokenStorage = FakeApiTokenStorage()
    val tokenRepository = ApiTokenRepository(tokenStorage, buildToken)
    val appConfig = testAppConfig()
    val adsController = FakeAdsController()
    val hiddenIssuesRepository = HiddenIssuesRepository(FakeHiddenIssuesStorage())
    val bookmarksRepository = BookmarksRepository(FakeBookmarksStorage())
    val snippetRepository = ReadmeSnippetRepository(readmeSource)
    val cacheRepository = RepoIssueGroupRepository(cacheStorage, Dispatchers.Unconfined)
    val searchRepository = IssueSearchRepository(api)

    /** A Top Repos state holder over the fakes above, running its work immediately on the calling thread. */
    val topReposStateHolder = TopReposStateHolder(
        modeRepository, tokenRepository, cacheRepository, searchRepository,
        CoroutineScope(Dispatchers.Unconfined),
    )

    /** A repo-issues state holder over the fakes above, running its work immediately on the calling thread. */
    val repoIssuesStateHolder = RepoIssuesStateHolder(
        modeRepository, cacheRepository, searchRepository, tokenRepository,
        CoroutineScope(Dispatchers.Unconfined),
    )

    /** A Top Repos ViewModel that gets the state holder and repositories above (pair with [StopKoinRule]). */
    fun topReposViewModel(config: AppConfig = appConfig): TopReposViewModel {
        restartKoin {
            single { topReposStateHolder }
            single { snippetRepository }
            single { hiddenIssuesRepository }
            single { tokenRepository }
            single<AdsController> { adsController }
            single { config }
            single<StringResolver> { FakeStringResolver }
        }
        return TopReposViewModel()
    }

    /** A repo-issues ViewModel that gets the state holder and repositories above (pair with [StopKoinRule]). */
    fun repoIssuesViewModel(): RepoIssuesViewModel {
        restartKoin {
            single { repoIssuesStateHolder }
            single { hiddenIssuesRepository }
            single { bookmarksRepository }
            single { tokenRepository }
            single<AdsController> { adsController }
            single { appConfig }
            single<StringResolver> { FakeStringResolver }
        }
        return RepoIssuesViewModel()
    }
}

/** A config for tests that do not care about its values. */
fun testAppConfig() = AppConfig(buildLabel = "test", splashAnimation = "coin", showDetailedDrawerMenu = false)

/** A search-API issue as GitHub would send it, in repo [owner]/[repo]. */
fun issueDto(id: Long, owner: String = "octo", repo: String = "repo") = IssueDto(
    id = id, number = id.toInt(), title = "Issue $id", body = "body", state = "open",
    htmlUrl = "https://github.com/$owner/$repo/issues/$id",
    repositoryUrl = "https://api.github.com/repos/$owner/$repo",
    comments = 0, updatedAt = "2026-10-01T12:00:00Z",
)

/** A successful search response carrying [issues] and a reported [total]. */
fun searchOk(issues: List<IssueDto>, total: Int = issues.size, rateLimit: RateLimitInfo? = null) = IssueSearchApiResponse(
    code = 200, isSuccessful = true,
    body = IssueSearchResponseDto(totalCount = total, items = issues),
    rateLimit = rateLimit,
)

/** A failed search response with HTTP [code]. */
fun searchFailed(code: Int, rateLimit: RateLimitInfo? = null) =
    IssueSearchApiResponse(code = code, isSuccessful = false, body = null, rateLimit = rateLimit)

/** A repo group holding one issue per id, as the app would build it from a search. */
fun repoGroupOf(owner: String, repo: String, vararg ids: Long): io.github.genesjc.githubissuefinder.model.RepoIssueGroup {
    val issues = ids.map { issueDto(it, owner, repo).toIssue(languageFilter = "kotlin") }
    return io.github.genesjc.githubissuefinder.model.RepoIssueGroup(
        repoOwner = owner,
        repoName = repo,
        repoFullName = "$owner/$repo",
        repoDisplayTitle = issues.first().repoDisplayTitle,
        issues = issues,
    )
}
