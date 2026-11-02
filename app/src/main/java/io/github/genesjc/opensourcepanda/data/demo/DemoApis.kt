package io.github.genesjc.opensourcepanda.data.demo

import io.github.genesjc.opensourcepanda.data.IssueSearchApi
import io.github.genesjc.opensourcepanda.data.IssueSearchApiResponse
import io.github.genesjc.opensourcepanda.data.ReadmeApi
import io.github.genesjc.opensourcepanda.data.ReadmeApiResponse
import io.github.genesjc.opensourcepanda.data.network.dto.IssueSearchResponseDto
import io.github.genesjc.opensourcepanda.data.network.dto.ReadmeDto
import io.github.genesjc.opensourcepanda.model.DEMO_TAG
import io.github.genesjc.opensourcepanda.model.RateLimitInfo
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

/**
 * A short README for a demo repo, encoded the way GitHub's Contents API returns one. It opens with the repo's
 * one-line description, so each card's README preview reads differently.
 */
@OptIn(ExperimentalEncodingApi::class)
class DemoReadmeApi : ReadmeApi {

    override suspend fun fetchReadme(repoOwner: String, repoName: String): ReadmeApiResponse {
        val description = DemoData.repos.firstOrNull { it.name == repoName }?.description.orEmpty()
        val markdown = "# $repoName\n\n$description\n\nThis README is demo data, shown because no GitHub token is set. " +
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
