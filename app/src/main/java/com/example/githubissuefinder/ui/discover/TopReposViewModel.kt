package com.example.githubissuefinder.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.githubissuefinder.data.RateLimitState
import com.example.githubissuefinder.data.parseRateLimitHeaders
import com.example.githubissuefinder.data.scoreAndRankRepos
import com.example.githubissuefinder.model.LanguageFilter
import com.example.githubissuefinder.model.Repo
import com.example.githubissuefinder.network.GitHubApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TOP_N = 20

/**
 * CM 1 of the repo-first "Discover" flow (option C from the architecture
 * discussion): loads a candidate pool of repos for the selected
 * language via repository search, scores them via scoreAndRankRepos(),
 * and keeps the top 20.
 *
 * v0.34: after the ranked list loads, separately annotates EACH visible
 * repo with its actual open "good first issue" count — the first half
 * of a two-step plan (repos-first, annotate with real counts, THEN a
 * later CM adds re-ranking/filtering based on those counts). This
 * directly addresses the known limitation flagged since CM 1: a repo
 * can rank highly on general health while having zero matching issues.
 * Reuses the existing searchIssues() endpoint/DTO — no new network
 * surface needed, since we only read totalCount from the response.
 *
 * RATE LIMIT NOTE: annotating all 20 visible repos means 20 additional
 * Search API calls (same bucket as the repo search and Feed's issue
 * search) per screen load, on top of the 1 repo-search call — 21 calls
 * in a burst, comfortably under the 30/min cap for a single load, but
 * worth knowing that switching the language filter twice within the
 * same minute would approach ~42 calls and risk a 403. Each repo's
 * count fails independently and silently (stays null) rather than
 * disrupting the rest of the list if one lookup errors.
 *
 * Reuses the same LanguageFilter enum Feed uses (v0.8), including its
 * ANY option, rather than a second parallel set of language options.
 * GitHub's search endpoint requires a non-empty query, so ANY
 * (queryValue = null) uses "stars:>0" — a qualifier every real repo
 * satisfies — instead of a language restriction, rather than silently
 * falling back to a specific language.
 *
 * No sample-data fallback yet (see TopReposUiState) — this screen
 * requires a configured GitHub token.
 */
class TopReposViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TopReposUiState())
    val uiState: StateFlow<TopReposUiState> = _uiState.asStateFlow()

    init {
        load(uiState.value.selectedLanguage)
    }

    fun onLanguageSelected(language: LanguageFilter) {
        _uiState.update { it.copy(selectedLanguage = language) }
        load(language)
    }

    fun retry() = load(uiState.value.selectedLanguage)

    private fun load(language: LanguageFilter) {
        if (!GitHubApiClient.hasToken) {
            _uiState.update { it.copy(noTokenConfigured = true, isLoading = false) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, noTokenConfigured = false) }
            try {
                val query = language.queryValue?.let { "language:$it" } ?: "stars:>0"
                val response = withContext(Dispatchers.IO) {
                    GitHubApiClient.api.searchRepositories(query = query)
                }
                val body = response.body()
                if (!response.isSuccessful || body == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "GitHub returned an error (HTTP ${response.code()})",
                        )
                    }
                    return@launch
                }
                RateLimitState.update(parseRateLimitHeaders(response.headers()))
                val ranked = scoreAndRankRepos(body.items).take(TOP_N)
                _uiState.update { it.copy(repos = ranked, isLoading = false) }
                annotateGoodFirstIssueCounts(ranked)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Couldn't load repos: ${e.message ?: "unknown error"}",
                    )
                }
            }
        }
    }

    /**
     * Fires one Search API call per repo, each independent — updates
     * that repo's card as soon as its own count arrives, rather than
     * waiting for all 20 before showing any of them.
     */
    private fun annotateGoodFirstIssueCounts(repos: List<Repo>) {
        repos.forEach { repo ->
            viewModelScope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        GitHubApiClient.api.searchIssues(
                            query = "repo:${repo.ownerLogin}/${repo.name} label:\"good first issue\" state:open",
                            perPage = 1,
                        )
                    }
                    val body = response.body()
                    if (response.isSuccessful && body != null) {
                        RateLimitState.update(parseRateLimitHeaders(response.headers()))
                        val count = body.totalCount
                        _uiState.update { state ->
                            state.copy(
                                repos = state.repos.map {
                                    if (it.id == repo.id) it.copy(goodFirstIssueCount = count) else it
                                },
                            )
                        }
                    }
                } catch (e: Exception) {
                    // Leave this repo's count as null (never loaded) on failure —
                    // one repo's lookup failing shouldn't disrupt the rest of the list.
                }
            }
        }
    }
}
