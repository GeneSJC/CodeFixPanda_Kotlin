package io.github.genesjc.codefixpanda.data.demo

import io.github.genesjc.codefixpanda.data.RepoIssueGroupCacheEntry
import io.github.genesjc.codefixpanda.data.RepoIssueGroupStorage

/**
 * The cache for demo data, kept in memory so demo results never reach the real cache file. It is
 * gone when the process ends, which is right for fake data.
 */
class InMemoryRepoIssueGroupStorage : RepoIssueGroupStorage {

    @Volatile
    private var entries: Map<String, RepoIssueGroupCacheEntry> = emptyMap()

    override fun readAll(): Map<String, RepoIssueGroupCacheEntry> = entries

    override fun writeAll(all: Map<String, RepoIssueGroupCacheEntry>) {
        entries = all
    }

    override fun clear() {
        entries = emptyMap()
    }
}
