package com.example.githubissuefinder.data

import com.example.githubissuefinder.model.Issue

/**
 * Plain in-memory cache of the most recently loaded search results, keyed
 * by id. Exists so Detail can look up a full Issue by id after Feed
 * navigates to it, without needing a second network call or a Room
 * database (that's still a future Commit Milestone). Cleared naturally on
 * process death — bookmarks are additionally snapshotted separately in
 * AppNavHost specifically so they survive being evicted from this cache
 * by a later search.
 */
object IssueCache {

    private val cache = mutableMapOf<Long, Issue>()

    fun putAll(issues: List<Issue>) {
        issues.forEach { cache[it.id] = it }
    }

    fun get(id: Long): Issue? = cache[id]
}
