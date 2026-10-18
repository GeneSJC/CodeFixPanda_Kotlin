package com.example.githubissuefinder.ui

import androidx.lifecycle.ViewModel
import com.example.githubissuefinder.data.AppConfig
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Shared parent for screen ViewModels: the app-level values every screen
 * may need, exposed as plain getters so a view only ever talks to its own
 * ViewModel.
 *
 * Today that is the app's build-time configuration (a view calls
 * viewModel.showDetailedDrawerMenu and knows nothing about BuildConfig or
 * where the value comes from). It is an immutable value object that Koin
 * supplies, and a test registers any [AppConfig] it likes.
 *
 * Being a KoinComponent is what lets every ViewModel declare what it
 * needs as a plain member, `private val x: X by inject()`, instead of a
 * constructor argument: a subclass just says ": BaseViewModel()", so the
 * default ViewModel factory can still create it.
 *
 * Why a base class and not a line in each ViewModel: the getters are
 * written once here, and a new constant is one getter in this file,
 * readable from every screen that extends it.
 *
 * What does NOT belong here: anything that reaches the data layer. A
 * ViewModel that needs a repository or a state holder declares it as its
 * own member and uses it itself. The base is for values, not for data
 * access or logic (and Kotlin allows one parent class, so a base that
 * collects odd helpers stops being a standard).
 */
abstract class BaseViewModel : ViewModel(), KoinComponent {

    private val appConfig: AppConfig by inject()

    /** "A.B", shown in the drawer header. */
    val buildLabel: String get() = appConfig.buildLabel

    /** Which start animation plays at launch and for the "Play App Start Animation" replay. */
    val splashAnimation: String get() = appConfig.splashAnimation

    /** True when the drawer lists every individual animation replay (a developer aid). */
    val showDetailedDrawerMenu: Boolean get() = appConfig.showDetailedDrawerMenu
}
