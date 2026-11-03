package io.github.genesjc.opensourcepanda.config

/**
 * The build-time constants the UI depends on, as one plain immutable object. A view never reads
 * BuildConfig: it gets these from its ViewModel (see BaseViewModel), and the Koin module reads
 * BuildConfig once to build this object.
 *
 * [buildLabel] is display-only but included anyway: "views never read BuildConfig" is easier to
 * follow without a carve-out, and it costs one forwarding getter.
 *
 * A test builds its own [AppConfig] (say, with the drawer flag on) with no rebuild.
 */
data class AppConfig(
    /** "A.B", from buildLabelA/buildLabelB in app/build.gradle.kts. */
    val buildLabel: String,
    /** Which start animation plays. */
    val splashAnimation: SplashAnimation,
    /** Whether the drawer lists every individual animation replay (a dev aid) or the single end-user entry. */
    val showDetailedDrawerMenu: Boolean,
)
