package com.example.githubissuefinder.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.githubissuefinder.data.RateLimitState
import com.example.githubissuefinder.data.parseRateLimitHeaders
import com.example.githubissuefinder.data.scoreAndRankRepos
import com.example.githubissuefinder.model.IssueLabelFilter
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
 * v0.36: onLanguageSelected() now returns a Boolean (accepted/blocked)
 * so TopReposScreen can show a Snackbar when a tap is actually blocked
 * by the cooldown, rather than the tap being silently swallowed.
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
 *
 * v0.41 ("Issue Mode" shell): the per-repo annotation is no longer
 * hardcoded to "good first issue" — it queries whichever label
 * IssueMode.current represents, read directly from that shared object
 * (see IssueMode.kt for why; same "read a plain shared value without a
 * live subscription" pattern GitHubApiClient.hasToken already uses
 * here). DiscoverNavHost guarantees a mode is chosen before this
 * ViewModel is ever created, so the fallback to GOOD_FIRST_ISSUE below
 * is defensive, not an expected path.
 *
 * onModeSelected() mirrors onLanguageSelected() exactly — same
 * cooldown, same accepted/blocked Boolean contract, same full reload —
 * because switching Mode costs exactly what switching language does:
 * a fresh repo search plus a fresh per-repo annotation pass. Both
 * share the same cooldown timer rather than tracking two separate
 * ones, since the underlying expense (one load()) is identical either
 * way. It writes IssueMode.current itself, and only once accepted —
 * see its own doc comment for why that ordering matters.
 *
 * NOT yet changed in this CM: still repo-search-first, annotated
 * per-repo after the fact — the same pipeline this screen has always
 * used, just parameterized by Mode instead of hardcoded. Replacing
 * this with issue-search-first (query issues directly, group by parent
 * repo, so a repo can never appear with zero matching issues) is a
 * separate, deliberately later CM — this one is only the Mode
 * state/UI shell.
 */
class TopReposViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TopReposUiState())
    val uiState: StateFlow<TopReposUiState> = _uiState.asStateFlow()

    private var lastLoadAtMs = 0L
    private var cooldownJob: Job? = null

    init {
        load(uiState.value.selectedLanguage)
    }

    /**
     * Returns true if the switch was accepted, false if it was blocked
     * by the cooldown (v0.36) — the caller (TopReposScreen) uses this to
     * decide whether to show a "please wait" Snackbar, so the ViewModel
     * doesn't need any special one-off-event machinery just for this.
     */
    fun onLanguageSelected(language: LanguageFilter): Boolean {
        if (isCoolingDown()) return false
        _uiState.update { it.copy(selectedLanguage = language) }
        load(language)
        return true
    }

    /**
     * Mirrors onLanguageSelected() exactly — see class doc comment for
     * why a Mode switch shares the exact same cooldown and reload logic
     * a language switch does. Deliberately does NOT write
     * IssueMode.current until AFTER confirming the switch is accepted
     * (same reason onLanguageSelected() only updates uiState's
     * selectedLanguage after that check) — writing it earlier would let
     * the top bar show a new mode while the list underneath silently
     * kept showing the OLD mode's data because the reload got blocked.
     */
    fun onModeSelected(mode: IssueLabelFilter): Boolean {
        if (isCoolingDown()) return false
        IssueMode.current = mode
        load(uiState.value.selectedLanguage)
        return true
    }

    private fun isCoolingDown(): Boolean {
        val elapsedMs = System.currentTimeMillis() - lastLoadAtMs
        // Still cooling down from the last load — ignore this switch
        // attempt rather than risk exceeding the Search API's 30/min
        // cap. The visible countdown (started by the previous load)
        // is already telling the user why; the caller additionally
        // surfaces a Snackbar on a blocked tap specifically.
        return lastLoadAtMs != 0L && elapsedMs < COOLDOWN_SECONDS * 1000L
    }

    /** Deliberately exempt from the cooldown — see class doc comment. */
    fun retry() = load(uiState.value.selectedLanguage)

    private fun load(language: LanguageFilter) {
        if (!GitHubApiClient.hasToken) {
            _uiState.update { it.copy(noTokenConfigured = true, isLoading = false) }
            return
        }

        // Defensive fallback only — DiscoverNavHost never creates this
        // screen (and therefore this ViewModel) while IssueMode.current
        // is null. See class doc comment.
        val activeMode = IssueMode.current ?: IssueLabelFilter.GOOD_FIRST_ISSUE

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
                annotateIssueCounts(ranked, activeMode)
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
     *
     * v0.41: queries `label` (whichever Issue Mode is active) instead
     * of a hardcoded "good first issue" string — renamed from
     * annotateGoodFirstIssueCounts to match. Writes to
     * Repo.issueCount, renamed from goodFirstIssueCount for the same
     * reason (see Repo.kt).
     */
    private fun annotateIssueCounts(repos: List<Repo>, label: IssueLabelFilter) {
        repos.forEach { repo ->
            viewModelScope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        GitHubApiClient.api.searchIssues(
                            query = "repo:${repo.ownerLogin}/${repo.name} label:\"${label.queryValue}\" state:open",
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
                                    if (it.id == repo.id) it.copy(issueCount = count) else it
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
