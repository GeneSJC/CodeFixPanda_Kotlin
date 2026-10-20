package com.example.githubissuefinder.ui.discover

import androidx.lifecycle.viewModelScope
import com.example.githubissuefinder.data.ApiTokenRepository
import com.example.githubissuefinder.data.HiddenIssuesRepository
import com.example.githubissuefinder.model.HiddenIssue
import com.example.githubissuefinder.ui.BaseViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.inject

/**
 * The Hidden screen's interface to the hidden issues: the list to show,
 * and Unhide / Unhide all. What is saved and when is
 * HiddenIssuesRepository's job; the confirmation dialog for "Unhide
 * all" is presentational state and stays in the screen. Only the issues whose tag matches the data
 * in use are listed (demo issues hidden in demo mode stay out of the real list, and the reverse).
 *
 * The repository is a plain member that Koin supplies; a test registers
 * one over a fake storage.
 */
class HiddenIssuesViewModel : BaseViewModel() {

    private val repository: HiddenIssuesRepository by inject()
    private val tokenRepository: ApiTokenRepository by inject()

    /** Hidden issues of the data in use, most recently hidden first. */
    val hiddenIssues: StateFlow<List<HiddenIssue>> = combine(repository.hiddenIssues, tokenRepository.activeTag) { hidden, tag ->
        hidden.filter { it.tag == tag }
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        repository.hiddenIssues.value.filter { it.tag == tokenRepository.activeTag.value },
    )

    fun unhide(issueId: Long) {
        repository.unhide(issueId)
    }

    fun unhideAll() {
        repository.unhideAll(tokenRepository.activeTag.value)
    }
}
