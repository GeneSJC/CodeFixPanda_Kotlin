package io.github.genesjc.opensourcepanda.data

import io.github.genesjc.opensourcepanda.testutil.issueDto
import org.junit.Assert.assertEquals
import org.junit.Test

/** IssueDto.toIssue: the owner and name come from the repository URL, and an unusable URL is an error. */
class IssueMappersTest {

    @Test
    fun theOwnerAndNameComeFromTheRepositoryUrl() {
        val issue = issueDto(1, "octo", "repo").toIssue(languageFilter = "kotlin")

        assertEquals("octo", issue.repoOwner)
        assertEquals("repo", issue.repoName)
    }

    @Test(expected = IllegalStateException::class)
    fun aRepositoryUrlWithNoOwnerIsAnError() {
        issueDto(1).copy(repositoryUrl = "repo").toIssue(languageFilter = null)
    }

    @Test(expected = IllegalStateException::class)
    fun aRepositoryUrlWithABlankNameIsAnError() {
        issueDto(1).copy(repositoryUrl = "https://api.github.com/repos/octo/ ").toIssue(languageFilter = null)
    }
}
