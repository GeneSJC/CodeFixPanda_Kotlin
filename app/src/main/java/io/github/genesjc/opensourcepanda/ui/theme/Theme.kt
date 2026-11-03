package io.github.genesjc.opensourcepanda.ui.theme

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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import io.github.genesjc.opensourcepanda.model.IssueLabelFilter

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
    primary = BrandOrangeMid,
    onPrimary = SurfaceLight,
    secondary = BrandOrangeMid,
    tertiary = BrandSeafoam,
    onTertiary = BrandNavy,
    tertiaryContainer = BrandSeafoamContainerLight,
    onTertiaryContainer = OnBrandSeafoamContainerLight,
    secondaryContainer = BrandContainerLight,
    onSecondaryContainer = OnBrandContainerLight,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = ModeHwCard,
    outline = ModeHwOutline,
    outlineVariant = ModeHwOutlineVariant,
    primaryContainer = ModeHwSecondaryContainer,
    onPrimaryContainer = ModeHwOnSecondaryContainer,
    inversePrimary = ModeHwSecondaryContainer,
    onBackground = ModeHwOnSurface,
    onSurface = ModeHwOnSurface,
    onSurfaceVariant = ModeHwOnSurfaceVariant,
    surfaceTint = ModeHwPrimary,
    inverseSurface = ModeHwInverseSurface,
    inverseOnSurface = ModeHwBackground,
    surfaceContainerLowest = ModeHwSurface,
    surfaceContainerLow = ModeHwSurfaceLow,
    surfaceContainer = ModeHwSurfaceContainer,
    surfaceContainerHigh = ModeHwCard,
    surfaceContainerHighest = ModeHwCard,
)

/**
 * How long switching Issue Mode takes to settle into its new scheme. A mode switch can happen
 * repeatedly in a session (unlike a splash), so it stays cheap: one animateColorAsState tween per
 * color role, and nothing keeps running once settled.
 */
private const val MODE_TRANSITION_MS = 350

@Composable
fun OpenSourcePandaTheme(
    // Deliberately not isSystemInDarkTheme(): the mode color schemes are light-only, so following the
    // system made the app flip from a dark gate screen to a light Top Repos. Hardcoded false until
    // there is an in-app dark-mode preference.
    darkTheme: Boolean = false,
    // Dynamic color (Android 12+) takes its palette from the wallpaper and would silently override our brand
    // colors, so it is off by default; a caller can still turn it on.
    dynamicColor: Boolean = false,
    // The active Issue Mode, read at the root in MainActivity through IssueModeViewModel. Null (no
    // mode chosen yet) gives the plain light/dark scheme: a mode adds to the baseline theme.
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

    CompositionLocalProvider(LocalModeDecor provides (issueMode?.decor() ?: DefaultModeDecor)) {
        MaterialTheme(
            colorScheme = targetScheme.animateAllColors(),
            typography = Typography,
            content = content
        )
    }
}

/**
 * Crossfades every role in a ColorScheme toward its new target over MODE_TRANSITION_MS when the
 * scheme changes (a mode switch). A soft crossfade suits a mode switch, where the list underneath is
 * just reloading; the splash uses an instant cut because its color changes land with a discrete
 * shape change. Only the roles the screens read are animated.
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
