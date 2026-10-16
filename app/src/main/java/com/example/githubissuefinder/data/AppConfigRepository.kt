package com.example.githubissuefinder.data

import com.example.githubissuefinder.BuildConfig

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
 * The only code that reads BuildConfig for UI purposes. The values are
 * fixed for the life of the process, so the config is built once, when
 * the singleton is first used, and never re-read.
 *
 * No Context and no init(context) are needed (BuildConfig is a
 * compile-time class), unlike the repositories that wrap storage. A test
 * builds its own repository around any [AppConfig], for example one with
 * the drawer flag switched on, with no rebuild.
 */
class AppConfigRepository(val config: AppConfig) {

    companion object {
        /** The app-wide instance, built from the real BuildConfig on first use. */
        val instance: AppConfigRepository by lazy {
            AppConfigRepository(
                AppConfig(
                    buildLabel = BuildConfig.BUILD_LABEL,
                    splashAnimation = BuildConfig.SPLASH_ANIMATION,
                    showDetailedDrawerMenu = BuildConfig.SHOW_DETAILED_DRAWER_MENU,
                ),
            )
        }
    }
}
