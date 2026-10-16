package com.example.githubissuefinder.data

import com.example.githubissuefinder.model.Issue
import com.example.githubissuefinder.model.RepoIssueGroup

/**
 * v0.51 ("Hide Issue"): the pure display-time filter both TopReposScreen
 * and RepoIssuesScreen apply against HiddenIssuesRepository.hiddenIds — a
 * standalone, top-level function (not private to either screen)
 * specifically so it's directly unit-testable with plain JUnit, same
 * reasoning as mergeReplacingByIssueId() in this same package: it
 * touches nothing Android/Compose-specific, so it doesn't need to.
 *
 * Deliberately a display-time filter, never a mutation of the cache —
 * see HiddenIssuesRepository's own doc comment. A group left with zero
 * visible issues after filtering is dropped entirely, matching the
 * spec's "a repo with no visible issues is not shown" rule — the same
 * rule RepoIssueGroup's own matchCount (issues.size) already relies on
 * downstream (a repo card's badge and Top Repos' repo-count chip both
 * read matchCount, so a filtered-to-empty group disappearing here is
 * what makes both update correctly with no separate logic needed).
 */
fun filterHiddenIssues(groups: List<RepoIssueGroup>, hiddenIds: Set<Long>): List<RepoIssueGroup> {
    if (hiddenIds.isEmpty()) return groups
    return groups
        .map { group -> group.copy(issues = group.issues.filterNot { it.id in hiddenIds }) }
        .filter { it.issues.isNotEmpty() }
}

/**
 * Single-repo counterpart of the above, for RepoIssuesScreen's own flat
 * issue list rather than a list of groups. NOT an overload of
 * filterHiddenIssues() above — List<Issue> and List<RepoIssueGroup>
 * erase to the same JVM signature (generics are erased at the bytecode
 * level, so both would compile to filterHiddenIssues(List, Set): List),
 * which Kotlin itself refuses to build ("platform declaration clash").
 * A distinct name sidesteps that entirely rather than reaching for
 * @JvmName just to keep an overload that erasure won't actually allow.
 */
fun filterHiddenIssuesFlat(issues: List<Issue>, hiddenIds: Set<Long>): List<Issue> {
    if (hiddenIds.isEmpty()) return issues
    return issues.filterNot { it.id in hiddenIds }
}
