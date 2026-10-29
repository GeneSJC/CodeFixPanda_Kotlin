package io.github.genesjc.opensourcepanda.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import io.github.genesjc.opensourcepanda.model.IssueLabelFilter

private val GfiColorScheme = lightColorScheme(
    primary = ModeGfiPrimary,
    onPrimary = ModeGfiOnPrimary,
    secondary = ModeGfiPrimary,
    tertiary = BrandSeafoam,
    onTertiary = SurfaceLight,
    tertiaryContainer = BrandSeafoamContainerLight,
    onTertiaryContainer = OnBrandSeafoamContainerLight,
    secondaryContainer = ModeGfiSecondaryContainer,
    onSecondaryContainer = ModeGfiOnSecondaryContainer,
    background = ModeGfiBackground,
    surface = ModeGfiSurface,
)

private val HwColorScheme = lightColorScheme(
    primary = ModeHwPrimary,
    onPrimary = ModeHwOnPrimary,
    secondary = ModeHwPrimary,
    tertiary = BrandSeafoam,
    onTertiary = SurfaceLight,
    tertiaryContainer = BrandSeafoamContainerLight,
    onTertiaryContainer = OnBrandSeafoamContainerLight,
    secondaryContainer = ModeHwSecondaryContainer,
    onSecondaryContainer = ModeHwOnSecondaryContainer,
    background = ModeHwBackground,
    surface = ModeHwSurface,
)

/**
 * Maps the active Issue Mode to its pastel ColorScheme. A `when` with no `else` on purpose: adding a
 * mode will not compile until it has colors, so a new mode cannot quietly inherit another's palette.
 * Light-only for now: dark mode would need a second, dark ColorScheme per mode, and Theme.kt would
 * select on mode crossed with light or dark.
 */
fun IssueLabelFilter.colorScheme(): ColorScheme = when (this) {
    IssueLabelFilter.GOOD_FIRST_ISSUE -> GfiColorScheme
    IssueLabelFilter.HELP_WANTED -> HwColorScheme
}
