package io.github.genesjc.opensourcepanda.model


/**
 * A single GitHub issue as the app displays it, mapped from the API response (see IssueMappers.kt).
 * repoDisplayTitle is a humanized repo name, a computed getter like repoFullName; see
 * RepoNameFormatting.kt for why it is an approximation. [tag] is null for real GitHub data and names
 * the source otherwise (DataTag.kt); it is saved with the issue, so a bookmark made on demo data stays
 * marked as demo.
 */
data class Issue(
    val id: Long,
    val title: String,
    val bodyText: String,
    val repoOwner: String,
    val repoName: String,
    val labels: List<IssueLabel>,
    val language: String?,
    val commentCount: Int,
    /** When GitHub last updated the issue (epoch millis); null if the timestamp could not be read. */
    val updatedAtMs: Long? = null,
    /** Public GitHub URL for this issue — used by the "Open in GitHub" action. */
    val htmlUrl: String,
    val tag: String? = null,
    /** The issue's number in its repo (the "#89"); 0 for one saved before this was recorded. */
    val number: Int = 0,
    /** When the issue was opened (epoch millis); null if unknown or saved before this was recorded. */
    val createdAtMs: Long? = null,
    /** The GitHub login of whoever opened it; null if unknown or saved before this was recorded. */
    val authorLogin: String? = null,
) {
    val repoFullName: String get() = "$repoOwner/$repoName"
    val repoDisplayTitle: String get() = humanizeRepoName(repoName)
}

data class IssueLabel(
    val name: String,
    /** Hex color as GitHub returns it, without the leading '#'. */
    val colorHex: String,
)
