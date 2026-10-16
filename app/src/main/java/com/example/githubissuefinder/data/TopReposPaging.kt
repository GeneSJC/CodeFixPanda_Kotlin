package com.example.githubissuefinder.data

/**
 * GitHub's Search API hard cap — it will not return results past this
 * point for any query, however many actually match (this is a
 * documented GitHub limitation, not something this app imposes).
 * canRequestNextPage() uses this to stop offering Load More before a
 * request would fail, rather than surfacing that as an error.
 */
internal const val SEARCH_API_RESULT_CAP = 1000

/**
 * False once there's nothing left worth requesting: either this
 * page came back short (fewer than TOP_REPOS_PER_PAGE items — the true end
 * of this query's results, not just an empty page), or the NEXT
 * page would start past GitHub's 1,000-result Search API cap.
 * Checked against the page just fetched, not the total the API
 * reports — total_count itself is only an estimate once a query
 * has more matches than the cap allows, so it isn't trustworthy
 * for this decision on its own.
 */
fun canRequestNextPage(fetchedThisPage: Int, justFetchedPage: Int): Boolean {
    if (fetchedThisPage < TOP_REPOS_PER_PAGE) return false
    val nextPageStartIndex = justFetchedPage * TOP_REPOS_PER_PAGE
    return nextPageStartIndex < SEARCH_API_RESULT_CAP
}
