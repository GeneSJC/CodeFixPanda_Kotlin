package com.example.githubissuefinder.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.githubissuefinder.model.IssueLabelFilter

private val DarkColorScheme = darkColorScheme(
    primary = BrandGreen80,
    onPrimary = OnGreenContainerDark,
    secondary = BrandPurple80,
    tertiary = BrandBlue80,
    secondaryContainer = GreenContainerDark,
    onSecondaryContainer = OnGreenContainerDark,
    background = BackgroundDark,
    surface = SurfaceDark,
)

private val LightColorScheme = lightColorScheme(
    primary = BrandGreen40,
    onPrimary = SurfaceLight,
    secondary = BrandPurple40,
    tertiary = BrandBlue40,
    secondaryContainer = GreenContainerLight,
    onSecondaryContainer = OnGreenContainerLight,
    background = BackgroundLight,
    surface = SurfaceLight,
)

/**
 * MODE CROSSFADE DURATION: how long switching Issue Mode takes to
 * visually settle into its new pastel scheme. A plain
 * animateColorAsState per color role, not a bespoke transition
 * sequence — this can happen repeatedly mid-session (unlike a splash,
 * a one-shot event), so it stays intentionally cheap: one tween per
 * color, no Animatable, nothing that keeps running once settled.
 */
private const val MODE_TRANSITION_MS = 350

@Composable
fun GitHubIssueFinderTheme(
    // Deliberately NOT isSystemInDarkTheme() — that was the real bug behind
    // a device-in-system-dark-mode user seeing a dark IssueModeGateScreen
    // (the only screen reachable with issueMode == null, so the only one
    // this param's value actually affects) followed immediately by an
    // always-light TopReposScreen (once a mode is picked, the `issueMode
    // != null -> issueMode.colorScheme()` branch below wins regardless of
    // darkTheme — Mode's own pastel schemes are light-only, per
    // ModeColorSchemes.kt). That made the app look like it randomly
    // flipped from dark to light between two consecutive screens, when
    // really only the gate screen was ever following the system setting.
    // Hardcoded false so the whole app defaults to light, consistently,
    // until a real in-app dark-mode preference exists to wire this to
    // instead.
    darkTheme: Boolean = false,
    // Dynamic color (Android 12+) derives its palette from the device's
    // wallpaper, which would silently override our brand colors below —
    // that's why earlier screenshots showed a generic purple/gray look
    // regardless of what Color.kt specified. Defaulting this to false so
    // our actual theme renders; callers can still opt back into dynamic
    // color explicitly if desired.
    dynamicColor: Boolean = false,
    // The active Issue Mode (v0.41), read at the root in MainActivity.kt
    // from the same shared IssueMode object the mode dropdown on
    // TopReposScreen writes to — see IssueMode.kt for why a shared
    // object rather than a threaded parameter. Null (no mode chosen
    // yet) falls back to the original light/dark scheme exactly as
    // before this CM — Mode is additive, not a replacement for the
    // app's baseline theme.
    issueMode: IssueLabelFilter? = null,
    content: @Composable () -> Unit
) {
    val targetScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        issueMode != null -> issueMode.colorScheme()
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = targetScheme.animateAllColors(),
        typography = Typography,
        content = content
    )
}

/**
 * Crossfades every role in a ColorScheme toward its new target value
 * over MODE_TRANSITION_MS whenever the scheme itself changes (i.e. on
 * a Mode switch), instead of the hard, instant swap OrigamiSplashScreen
 * uses for its per-fold colors. Deliberately different choices for
 * deliberately different situations: the splash's color changes land
 * in lockstep with a shape already changing discretely at that exact
 * moment, so an instant cut reads as "this is a new fold." A Mode
 * switch has no such accompanying discrete event — the repo list
 * underneath is just reloading — so a soft crossfade reads as "the
 * whole screen is settling into its new mode" rather than a jarring
 * flash. Only the handful of roles this app's screens actually read
 * (RepoCard, TopAppBar, etc. via MaterialTheme.colorScheme.*) are
 * animated — every ColorScheme role still gets a value either way,
 * this just controls how smoothly each one arrives.
 */
@Composable
private fun ColorScheme.animateAllColors(): ColorScheme {
    val animSpec = tween<androidx.compose.ui.graphics.Color>(durationMillis = MODE_TRANSITION_MS)
    return copy(
        primary = animateColorAsState(primary, animSpec, label = "primary").value,
        onPrimary = animateColorAsState(onPrimary, animSpec, label = "onPrimary").value,
        secondary = animateColorAsState(secondary, animSpec, label = "secondary").value,
        tertiary = animateColorAsState(tertiary, animSpec, label = "tertiary").value,
        secondaryContainer = animateColorAsState(secondaryContainer, animSpec, label = "secondaryContainer").value,
        onSecondaryContainer = animateColorAsState(onSecondaryContainer, animSpec, label = "onSecondaryContainer").value,
        background = animateColorAsState(background, animSpec, label = "background").value,
        surface = animateColorAsState(surface, animSpec, label = "surface").value,
    )
}
