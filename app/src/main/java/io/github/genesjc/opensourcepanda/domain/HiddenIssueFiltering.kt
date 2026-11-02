package io.github.genesjc.opensourcepanda.domain

import io.github.genesjc.opensourcepanda.model.Issue
import io.github.genesjc.opensourcepanda.model.RepoIssueGroup

/**
 * The display-time filter that Top Repos and the repo screen apply against the hidden ids; it never
 * changes the cache. A pure top-level function so it is unit-testable directly. Every repo stays
 * in the result, even one whose issues are all hidden (its group comes back with no issues and the
 * card shows "All hidden"): a repo leaves the list only when the cache is cleared.
 */
fun filterHiddenIssues(groups: List<RepoIssueGroup>, hiddenIds: Set<Long>): List<RepoIssueGroup> {
    if (hiddenIds.isEmpty()) return groups
    return groups.map { group -> group.copy(issues = group.issues.filterNot { it.id in hiddenIds }) }
}

/**
 * The same filter for the repo screen's flat issue list. Not an overload: List<Issue> and
 * List<RepoIssueGroup> erase to the same JVM signature, which Kotlin refuses to compile.
 */
fun filterHiddenIssuesFlat(issues: List<Issue>, hiddenIds: Set<Long>): List<Issue> {
    if (hiddenIds.isEmpty()) return issues
    return issues.filterNot { it.id in hiddenIds }
}
