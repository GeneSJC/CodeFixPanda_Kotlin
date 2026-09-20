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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TOP_N = 20
private const val COOLDOWN_SECONDS = 60

/**
 * CM 1 of the repo-first "Discover" flow (option C from the architecture
 * discussion): loads a candidate pool of repos for the selected
 * language via repository search, scores them via scoreAndRankRepos(),
 * and keeps the top 20.
 *
 * v0.34: annotates each visible repo with its actual open
 * good-first-issue count (a separate call per repo).
 *
 * v0.35: adds a 60-second cooldown between loads, guarding
 * onLanguageSelected() specifically. This is NOT "block after the 2nd
 * switch in a minute" — it's "every load starts a 60s cooldown before
 * the next one," because the arithmetic makes those the same rule in
 * practice: one load is already 21 Search API calls (1 repo search +
 * 20 per-repo issue checks from v0.34), and a SECOND load within that
 * same 60-second window already reaches ~42 calls — over the 30/min
 * cap on the second attempt, not the third. Guarding every load
 * (including the very first, on screen-open) rather than only
 * "switches" reflects that the expensive thing is the load itself, not
 * specifically the act of changing languages.
 *
 * retry() is DELIBERATELY exempt from the cooldown: a failed load may
 * not have fired all 20 per-repo calls in the first place (e.g. if the
 * repo search itself failed, zero of them did), so blocking a genuine
 * error-recovery retry for a full minute seemed like the wrong
 * tradeoff. This is a real design choice, not an oversight.
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

    private var lastLoadAtMs = 0L
    private var cooldownJob: Job? = null

    init {
        load(uiState.value.selectedLanguage)
    }

    fun onLanguageSelected(language: LanguageFilter) {
        val elapsedMs = System.currentTimeMillis() - lastLoadAtMs
        if (lastLoadAtMs != 0L && elapsedMs < COOLDOWN_SECONDS * 1000L) {
            // Still cooling down from the last load — ignore this switch
            // attempt rather than risk exceeding the Search API's 30/min
            // cap. The visible countdown (started by the previous load)
            // is already telling the user why.
            return
        }
        _uiState.update { it.copy(selectedLanguage = language) }
        load(language)
    }

    /** Deliberately exempt from the cooldown — see class doc comment. */
    fun retry() = load(uiState.value.selectedLanguage)

    private fun load(language: LanguageFilter) {
        if (!GitHubApiClient.hasToken) {
            _uiState.update { it.copy(noTokenConfigured = true, isLoading = false) }
            return
        }

        lastLoadAtMs = System.currentTimeMillis()
        startCooldownCountdown()

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

    private fun startCooldownCountdown() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            var remaining = COOLDOWN_SECONDS
            while (remaining > 0) {
                _uiState.update { it.copy(cooldownSecondsRemaining = remaining) }
                delay(1000)
                remaining--
            }
            _uiState.update { it.copy(cooldownSecondsRemaining = 0) }
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
