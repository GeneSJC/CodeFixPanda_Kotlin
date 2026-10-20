package com.example.githubissuefinder.data.demo

import com.example.githubissuefinder.data.IssueSearchApi
import com.example.githubissuefinder.data.IssueSearchApiResponse
import com.example.githubissuefinder.data.RateLimitInfo
import com.example.githubissuefinder.data.ReadmeApi
import com.example.githubissuefinder.data.ReadmeApiResponse
import com.example.githubissuefinder.model.DEMO_TAG
import com.example.githubissuefinder.network.dto.IssueSearchResponseDto
import com.example.githubissuefinder.network.dto.ReadmeDto
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private val LABEL = Regex("""label:"([^"]+)"""")
private val LANGUAGE = Regex("""language:(\S+)""")
private val REPO = Regex("""repo:(\S+)""")

/**
 * Answers the app's search queries from [DemoData]: it reads the same `label:`, `language:` and
 * `repo:` qualifiers the real API is sent and filters the fake issues, so Top Repos, language
 * filtering and a single repo's refresh all behave as they do with real data. It has no rate limit
 * to report, so the chip stays at its placeholder.
 */
class DemoIssueSearchApi : IssueSearchApi {

    override suspend fun searchIssues(query: String, perPage: Int, page: Int): IssueSearchApiResponse {
        val label = LABEL.find(query)?.groupValues?.get(1)
        val language = LANGUAGE.find(query)?.groupValues?.get(1)
        val repo = REPO.find(query)?.groupValues?.get(1)
        val matches = DemoData.issues.filter { demo ->
            (label == null || demo.dto.labels.any { it.name == label }) &&
                (language == null || demo.language == language) &&
                (repo == null || demo.repoFullName == repo)
        }
        val items = matches.drop((page - 1) * perPage).take(perPage).map { it.dto }
        return IssueSearchApiResponse(
            code = 200,
            isSuccessful = true,
            body = IssueSearchResponseDto(totalCount = matches.size, items = items),
            rateLimit = null,
            tag = DEMO_TAG,
        )
    }

    override suspend fun fetchRateLimit(): RateLimitInfo? = null
}

/** A short README for any demo repo, encoded the way GitHub's Contents API returns one. */
@OptIn(ExperimentalEncodingApi::class)
class DemoReadmeApi : ReadmeApi {

    override suspend fun getReadme(repoOwner: String, repoName: String): ReadmeApiResponse {
        val markdown = "# $repoName\n\nThis README is demo data, shown because no GitHub token is set. " +
            "Add a token in Settings to see real repositories and their READMEs.\n"
        val dto = ReadmeDto(
            name = "README.md",
            content = Base64.encode(markdown.toByteArray(Charsets.UTF_8)),
            encoding = "base64",
            htmlUrl = null,
        )
        return ReadmeApiResponse(code = 200, isSuccessful = true, body = dto)
    }
}
