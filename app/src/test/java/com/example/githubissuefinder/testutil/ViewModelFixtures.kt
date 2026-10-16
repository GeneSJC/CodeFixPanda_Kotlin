package com.example.githubissuefinder.testutil

import com.example.githubissuefinder.data.ApiTokenRepository
import com.example.githubissuefinder.data.ApiTokenStorage
import com.example.githubissuefinder.data.BookmarksRepository
import com.example.githubissuefinder.data.BookmarksStorage
import com.example.githubissuefinder.data.HiddenIssuesRepository
import com.example.githubissuefinder.data.HiddenIssuesStorage
import com.example.githubissuefinder.data.IssueModeRepository
import com.example.githubissuefinder.data.IssueModeStorage
import com.example.githubissuefinder.data.IssueSearchApi
import com.example.githubissuefinder.data.IssueSearchApiResponse
import com.example.githubissuefinder.data.IssueSearchRepository
import com.example.githubissuefinder.data.RateLimitInfo
import com.example.githubissuefinder.data.ReadmeFetch
import com.example.githubissuefinder.data.ReadmeSnippetRepository
import com.example.githubissuefinder.data.ReadmeSource
import com.example.githubissuefinder.data.RepoIssueGroupCacheEntry
import com.example.githubissuefinder.data.RepoIssueGroupRepository
import com.example.githubissuefinder.data.RepoIssueGroupStorage
import com.example.githubissuefinder.data.TopReposService
import com.example.githubissuefinder.data.toIssue
import com.example.githubissuefinder.model.HiddenIssue
import com.example.githubissuefinder.model.Issue
import com.example.githubissuefinder.model.IssueLabelFilter
import com.example.githubissuefinder.network.dto.IssueDto
import com.example.githubissuefinder.network.dto.IssueSearchResponseDto
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

/** Everything a Top Repos or repo-issues ViewModel (or the Top Repos service) is built from, wired over fakes, with the pieces a test needs to poke at. */
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
    val hiddenIssuesRepository = HiddenIssuesRepository(FakeHiddenIssuesStorage())
    val bookmarksRepository = BookmarksRepository(FakeBookmarksStorage())
    val snippetRepository = ReadmeSnippetRepository(readmeSource)
    val cacheRepository = RepoIssueGroupRepository(cacheStorage, Dispatchers.Unconfined)
    val searchRepository = IssueSearchRepository(api, { tokenRepository.effectiveToken.value.isNotBlank() })

    /** A Top Repos service over the fakes above, running its work immediately on the calling thread. */
    val topReposService = TopReposService(
        modeRepository, tokenRepository, cacheRepository, searchRepository,
        CoroutineScope(Dispatchers.Unconfined),
    )

    /** Makes TopReposViewModel use [topReposService]; pair with [uninstall] in teardown. */
    fun installTopReposService() {
        TopReposService.replaceInstanceForTests(topReposService)
    }

    /** Makes the hidden-issue and bookmark repositories that BaseViewModel reads the fakes above; pair with [uninstall] in teardown. */
    fun installRepositories() {
        HiddenIssuesRepository.replaceInstanceForTests(hiddenIssuesRepository)
        BookmarksRepository.replaceInstanceForTests(bookmarksRepository)
    }

    companion object {
        /** Removes everything the install functions put in place. Call it in teardown. */
        fun uninstall() {
            TopReposService.replaceInstanceForTests(null)
            HiddenIssuesRepository.replaceInstanceForTests(null)
            BookmarksRepository.replaceInstanceForTests(null)
        }
    }
}

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
fun repoGroupOf(owner: String, repo: String, vararg ids: Long): com.example.githubissuefinder.model.RepoIssueGroup {
    val issues = ids.map { issueDto(it, owner, repo).toIssue(languageFilter = "kotlin") }
    return com.example.githubissuefinder.model.RepoIssueGroup(
        repoOwner = owner,
        repoName = repo,
        repoFullName = "$owner/$repo",
        repoDisplayTitle = issues.first().repoDisplayTitle,
        issues = issues,
    )
}
