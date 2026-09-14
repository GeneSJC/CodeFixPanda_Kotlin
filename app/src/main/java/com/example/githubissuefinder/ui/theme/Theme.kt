package com.example.githubissuefinder.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

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

@Composable
fun GitHubIssueFinderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color (Android 12+) derives its palette from the device's
    // wallpaper, which would silently override our brand colors below —
    // that's why earlier screenshots showed a generic purple/gray look
    // regardless of what Color.kt specified. Defaulting this to false so
    // our actual theme renders; callers can still opt back into dynamic
    // color explicitly if desired.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
