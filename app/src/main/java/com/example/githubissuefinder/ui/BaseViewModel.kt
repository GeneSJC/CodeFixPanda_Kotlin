package com.example.githubissuefinder.ui

import androidx.lifecycle.ViewModel
import com.example.githubissuefinder.data.AppConfig
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * The common parent of every screen ViewModel. It holds app-level values only (today the build
 * configuration, an immutable AppConfig that Koin supplies) as plain getters, so a view only ever
 * talks to its own ViewModel. Being a KoinComponent lets every ViewModel declare its dependencies as
 * plain members (`private val x: X by inject()`) and keep a no-argument constructor.
 * Nothing that reaches the data layer belongs here: a ViewModel that needs a repository or state
 * holder declares it itself. Kotlin allows one parent class, so a base that collects odd helpers
 * stops being a standard.
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
