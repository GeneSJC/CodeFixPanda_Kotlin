package com.example.githubissuefinder.data


/**
 * The build-time constants the UI depends on, bundled into one plain
 * immutable object. A view never reads BuildConfig itself: it gets these
 * values from its ViewModel (see BaseViewModel), so the build system
 * stays on the far side of one crossing point, [AppConfigRepository].
 *
 * Why include [buildLabel] even though it is display-only: one rule
 * ("views never read BuildConfig") is easier to follow and to review
 * than a rule with a carve-out, and the cost is one forwarding getter.
 */
data class AppConfig(
    /** "A.B", from buildLabelA/buildLabelB in app/build.gradle.kts. */
    val buildLabel: String,
    /** Which start animation plays: "coin", "origami", "shapeStack" or "titleReveal". */
    val splashAnimation: String,
    /** Whether the drawer lists every individual animation replay (a dev aid) or the single end-user entry. */
    val showDetailedDrawerMenu: Boolean,
)

/**
 * Holds the build-time configuration for UI purposes. The values are
 * fixed for the life of the process; the Koin module reads BuildConfig
 * once to build the config, so that line is the only place that does.
 * A test builds its own repository around any [AppConfig], for example
 * one with the drawer flag switched on, with no rebuild.
 */
class AppConfigRepository(val config: AppConfig) {
}
