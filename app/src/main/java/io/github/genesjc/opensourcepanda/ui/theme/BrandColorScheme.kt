package io.github.genesjc.opensourcepanda.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * The app's identity colors as a color scheme, for the screens that stand for the app itself (the
 * splash today) and not for an Issue Mode. White text sits on the orange, and the sun yellow is the
 * accent: the burst of light behind the panda.
 */
val BrandColorScheme = lightColorScheme(
    primary = BrandSun,
    onPrimary = BrandNavy,
    secondary = Color.White,
    background = BrandOrangeMid,
    onBackground = Color.White,
    surface = BrandOrangeMid,
    onSurface = Color.White,
    onSurfaceVariant = BrandSoftWhite,
)
