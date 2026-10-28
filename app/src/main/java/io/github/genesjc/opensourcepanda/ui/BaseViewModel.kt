package io.github.genesjc.opensourcepanda.ui

import androidx.lifecycle.ViewModel
import io.github.genesjc.opensourcepanda.data.AppConfig
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * The common parent of every screen ViewModel. It holds app-wide values and cross-cutting helpers
 * that any ViewModel may need: today the build configuration (an immutable AppConfig that Koin
 * supplies) as plain getters, and [string] for resolving string resources. Logging would be another
 * fit. A view only ever talks to its own ViewModel. Being a KoinComponent lets every ViewModel
 * declare its dependencies as plain members (`private val x: X by inject()`) and keep a no-argument
 * constructor.
 * Nothing feature-specific belongs here, and nothing that reaches the data layer: a ViewModel that
 * needs a repository or state holder declares it itself. A helper earns a place only if most
 * ViewModels could plausibly use it and it knows nothing about one feature.
 */
abstract class BaseViewModel : ViewModel(), KoinComponent {

    private val appConfig: AppConfig by inject()

    /**
     * Resolves a string resource for wording this ViewModel builds, e.g. `string(R.string.error_http, code)`.
     * Text the ViewModel has already mapped keeps its language until the state is next recomputed.
     */
    protected val string: StringResolver by inject()

    /** "A.B", shown in the drawer header. */
    val buildLabel: String get() = appConfig.buildLabel

    /** Which start animation plays at launch and for the "Play App Start Animation" replay. */
    val splashAnimation: String get() = appConfig.splashAnimation

    /** True when the drawer lists every individual animation replay (a developer aid). */
    val showDetailedDrawerMenu: Boolean get() = appConfig.showDetailedDrawerMenu
}
