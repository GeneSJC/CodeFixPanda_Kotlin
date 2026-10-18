package com.example.githubissuefinder.ui

import androidx.lifecycle.ViewModel
import com.example.githubissuefinder.data.AppConfigRepository
import com.example.githubissuefinder.data.BookmarksRepository
import com.example.githubissuefinder.data.HiddenIssuesRepository
import com.example.githubissuefinder.model.Issue
import kotlinx.coroutines.flow.StateFlow

/**
 * Shared parent for screen ViewModels: what several screens need from
 * the app as a whole, exposed as plain getters so a view only ever talks
 * to its own ViewModel.
 *
 * Today that is the app's build-time configuration (a view calls
 * viewModel.showDetailedDrawerMenu and knows nothing about BuildConfig,
 * AppConfig or where the value comes from), and the hidden-issue and
 * bookmark pass-throughs that more than one screen uses.
 *
 * Why a base class and not a line in each ViewModel: the repository
 * argument, its storage and the getters are written once here. A
 * subclass just says ": BaseViewModel()" and a new constant is one
 * getter in this file, readable from every screen that extends it.
 *
 * Why the repository argument defaults to the app-wide instance: the
 * subclass needs no extra code in production, and a ViewModel whose
 * constructor arguments all have defaults still has the no-argument
 * constructor the default factory needs. A subclass that wants to be
 * tested with a different config takes an AppConfigRepository argument
 * of its own and passes it up.
 *
 * What belongs here is decided by one question: is this app-level, or
 * specific to one view? App-level data or capability that a ViewModel
 * would reach for (the build config is the model case) belongs here even
 * if only one ViewModel uses it today; that is the standard. Anything
 * specific to one screen's behavior stays in that ViewModel or in a
 * collaborator it takes through its constructor. Stateful or
 * coroutine-launching helpers are plain delegate classes as well (Kotlin
 * allows one parent class, so this slot is scarce, and a base class that
 * collects odd helpers stops being a standard).
 */
abstract class BaseViewModel(
    private val appConfigRepository: AppConfigRepository = AppConfigRepository.instance,
) : ViewModel() {

    /** "A.B", shown in the drawer header. */
    val buildLabel: String get() = appConfigRepository.config.buildLabel

    /** Which start animation plays at launch and for the "Play App Start Animation" replay. */
    val splashAnimation: String get() = appConfigRepository.config.splashAnimation

    /** True when the drawer lists every individual animation replay (a developer aid). */
    val showDetailedDrawerMenu: Boolean get() = appConfigRepository.config.showDetailedDrawerMenu

    /**
     * Ids the user hid, to filter hidden issues out of a list at display
     * time. Shared by the screens that list issues (promoted here when a
     * second ViewModel needed it: the rule of two). A pass-through, so it
     * lives here and not in a state holder; resolved when read, so a ViewModel
     * that never uses it needs the repository to be neither set up nor
     * installed. Tests install their own with
     * HiddenIssuesRepository.replaceInstanceForTests().
     */
    val hiddenIds: StateFlow<Set<Long>> get() = HiddenIssuesRepository.instance.hiddenIds

    /** Ids of bookmarked issues, to draw each issue card's star. */
    val bookmarkedIds: StateFlow<Set<Long>> get() = BookmarksRepository.instance.bookmarkedIds

    /** Toggles [issue]'s bookmark; returns the new state (true = now bookmarked). */
    fun toggleBookmark(issue: Issue): Boolean = BookmarksRepository.instance.toggle(issue)
}
