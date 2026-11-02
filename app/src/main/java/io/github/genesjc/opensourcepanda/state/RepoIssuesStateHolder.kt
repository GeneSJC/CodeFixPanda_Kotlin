package io.github.genesjc.opensourcepanda.state

import io.github.genesjc.opensourcepanda.data.ApiTokenRepository
import io.github.genesjc.opensourcepanda.data.IssueModeRepository
import io.github.genesjc.opensourcepanda.data.IssueSearchRepository
import io.github.genesjc.opensourcepanda.data.IssueSearchResult
import io.github.genesjc.opensourcepanda.data.RepoIssueGroupRepository
import io.github.genesjc.opensourcepanda.domain.resolveActiveMode
import io.github.genesjc.opensourcepanda.model.IssueLabelFilter
import io.github.genesjc.opensourcepanda.model.LanguageFilter
import io.github.genesjc.opensourcepanda.model.RepoIssueGroup
import io.github.genesjc.opensourcepanda.model.SearchFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The state of the repo being viewed: its issues, whether a refresh is running, the last result,
 * and the logic that changes it. RepoIssuesViewModel maps it to what the screen shows.
 *
 * It shows only the issues already in the repo's group, so opening a repo makes no network call.
 * refreshThisRepo() is the one explicit, user-initiated exception: a single call for the repo's
 * open issues for the active mode and language (up to GitHub's 100 per call). It replaces the held
 * issues with the complete result and writes the updated group back into the Top Repos cache, so
 * the card's match count agrees on return; an empty result removes the repo from the cache. Top
 * Repos picks the change up with syncFromCache(). A repo with more than 100 open matches for one
 * label would still look partial; that is rare and deliberately not chased.
 *
 * An app-wide singleton, because the state must outlive a composition. open() starts a fresh visit:
 * it clears the state and cancels a refresh left over from the previous one. The work runs in the
 * shared [AppScope]; the Koin module builds the instance. More in docs/design-notes.md,
 * "Repo issues state".
 */
class RepoIssuesStateHolder(
    private val modeRepository: IssueModeRepository,
    private val cacheRepository: RepoIssueGroupRepository,
    private val searchRepository: IssueSearchRepository,
    private val tokenRepository: ApiTokenRepository,
    private val scope: CoroutineScope = AppScope.scope,
) {

    private val _state = MutableStateFlow(RepoIssuesState())
    val state: StateFlow<RepoIssuesState> = _state.asStateFlow()

    /** The active Issue Mode, or null while none is chosen (which DiscoverNavHost prevents from ever being observed here). */
    val mode: StateFlow<IssueLabelFilter?> = modeRepository.mode

    private var loadedForRepoFullName: String? = null

    /** Parent of everything one visit to the screen starts; open() cancels it and starts a new one. */
    private var visitJob: Job = SupervisorJob()

    /** Starts a fresh visit: clears the state and cancels a refresh left over from the previous visit. */
    fun open() {
        visitJob.cancel()
        visitJob = SupervisorJob()
        loadedForRepoFullName = null
        _state.value = RepoIssuesState()
    }

    fun loadIfNeeded(repoGroup: RepoIssueGroup) {
        if (loadedForRepoFullName == repoGroup.repoFullName) return
        loadedForRepoFullName = repoGroup.repoFullName
        _state.update { it.copy(issues = repoGroup.issues) }
    }

    /**
     * The one explicit network call on this screen. A no-op while a refresh is in flight; re-tappable,
     * so it is not gated on hasRefreshed.
     */
    fun refreshThisRepo(repoGroup: RepoIssueGroup, language: LanguageFilter) {
        if (_state.value.isRefreshing) return

        // Defensive fallback only: DiscoverNavHost ensures a mode is chosen before this screen.
        val activeMode = resolveActiveMode(modeRepository.mode.value)

        scope.launch(visitJob) {
            _state.update { it.copy(isRefreshing = true, refreshFailure = null) }
            try {
                when (val result = searchRepository.searchRepoIssues(repoGroup.repoFullName, activeMode, language)) {
                    is IssueSearchResult.Success -> {
                        val completeIssues = result.page.issues
                        val refreshedAtMs = System.currentTimeMillis()
                        _state.update {
                            it.copy(
                                issues = completeIssues,
                                isRefreshing = false,
                                hasRefreshed = true,
                                lastRefreshedAtMs = refreshedAtMs,
                            )
                        }
                        cacheRepository.replaceRepoGroup(
                            language = language,
                            mode = activeMode,
                            repoFullName = repoGroup.repoFullName,
                            updatedGroup = if (completeIssues.isEmpty()) null else repoGroup.copy(issues = completeIssues),
                        )
                    }
                    is IssueSearchResult.HttpError -> _state.update {
                        it.copy(isRefreshing = false, refreshFailure = SearchFailure.Http(result.code))
                    }
                    is IssueSearchResult.Failed -> showRefreshFailure(result.message)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showRefreshFailure(e.message)
            }
        }
    }

    private fun showRefreshFailure(detail: String?) {
        _state.update { it.copy(isRefreshing = false, refreshFailure = SearchFailure.Failed(detail)) }
    }
}
