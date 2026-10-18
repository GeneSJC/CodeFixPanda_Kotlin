package com.example.githubissuefinder.ui

import androidx.lifecycle.ViewModel
import com.example.githubissuefinder.data.AppConfig

/**
 * Shared parent for screen ViewModels: the app-level values every screen
 * may need, exposed as plain getters so a view only ever talks to its own
 * ViewModel.
 *
 * Today that is the app's build-time configuration (a view calls
 * viewModel.showDetailedDrawerMenu and knows nothing about BuildConfig or
 * where the value comes from). It is an immutable value object, supplied
 * by the Koin module, and a test passes any [AppConfig] it likes.
 *
 * Why a base class and not a line in each ViewModel: the getters are
 * written once here, and a new constant is one getter in this file,
 * readable from every screen that extends it.
 *
 * What does NOT belong here: anything that reaches the data layer. A
 * ViewModel that needs a repository or a state holder takes it in its own
 * constructor and uses it itself. The base is for values, not for data
 * access or logic (and Kotlin allows one parent class, so a base that
 * collects odd helpers stops being a standard).
 */
abstract class BaseViewModel(
    private val appConfig: AppConfig,
) : ViewModel() {

    /** "A.B", shown in the drawer header. */
    val buildLabel: String get() = appConfig.buildLabel

    /** Which start animation plays at launch and for the "Play App Start Animation" replay. */
    val splashAnimation: String get() = appConfig.splashAnimation

    /** True when the drawer lists every individual animation replay (a developer aid). */
    val showDetailedDrawerMenu: Boolean get() = appConfig.showDetailedDrawerMenu
}
