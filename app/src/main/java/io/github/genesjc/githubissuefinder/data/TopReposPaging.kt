package io.github.genesjc.githubissuefinder.data

/**
 * GitHub's Search API hard cap — it will not return results past this
 * point for any query, however many actually match (this is a
 * documented GitHub limitation, not something this app imposes).
 * canRequestNextPage() uses this to stop offering Load More before a
 * request would fail, rather than surfacing that as an error.
 */
internal const val SEARCH_API_RESULT_CAP = 1000

/**
 * False once there is nothing left worth requesting: the page came back short (the true end of the
 * results), or the next page would start past GitHub's 1,000-result cap. Checked against the page
 * just fetched and not total_count, which is only an estimate once matches exceed the cap.
 */
fun canRequestNextPage(fetchedThisPage: Int, justFetchedPage: Int): Boolean {
    if (fetchedThisPage < TOP_REPOS_PER_PAGE) return false
    val nextPageStartIndex = justFetchedPage * TOP_REPOS_PER_PAGE
    return nextPageStartIndex < SEARCH_API_RESULT_CAP
}
