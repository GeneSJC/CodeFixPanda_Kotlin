package io.github.genesjc.githubissuefinder.data

/**
 * The build-time constants the UI depends on, bundled into one plain
 * immutable object. A view never reads BuildConfig itself: it gets these
 * values from its ViewModel (see BaseViewModel), so the build system
 * stays on the far side of one crossing point, the Koin module, which
 * reads BuildConfig once to build this object.
 *
 * Why include [buildLabel] even though it is display-only: one rule
 * ("views never read BuildConfig") is easier to follow and to review
 * than a rule with a carve-out, and the cost is one forwarding getter.
 *
 * A test builds its own [AppConfig], for example one with the drawer flag
 * switched on, with no rebuild.
 */
data class AppConfig(
    /** "A.B", from buildLabelA/buildLabelB in app/build.gradle.kts. */
    val buildLabel: String,
    /** Which start animation plays: "coin", "origami", "shapeStack" or "titleReveal". */
    val splashAnimation: String,
    /** Whether the drawer lists every individual animation replay (a dev aid) or the single end-user entry. */
    val showDetailedDrawerMenu: Boolean,
)
