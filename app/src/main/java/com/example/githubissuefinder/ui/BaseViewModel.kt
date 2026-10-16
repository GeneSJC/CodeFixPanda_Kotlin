package com.example.githubissuefinder.ui

import androidx.lifecycle.ViewModel
import com.example.githubissuefinder.data.AppConfigRepository

/**
 * Shared parent for screen ViewModels whose views need the app's
 * build-time configuration. It exposes each config value as a plain
 * scalar getter, so a view calls viewModel.showDetailedDrawerMenu and
 * knows nothing about BuildConfig, AppConfig or where the value comes
 * from.
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
 * What belongs here is a judgment call, not a head count: a standard
 * capability that several screen ViewModels share, even if not all of
 * them use it. Anything stateful, coroutine-launching, or specific to
 * one or two screens is a plain delegate class that the ViewModel takes
 * through its constructor instead (Kotlin allows one parent class, so
 * this slot is scarce, and a base class that collects odd helpers stops
 * being a standard).
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
}
