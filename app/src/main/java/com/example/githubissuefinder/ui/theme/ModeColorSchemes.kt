package com.example.githubissuefinder.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import com.example.githubissuefinder.model.IssueLabelFilter

private val GfiColorScheme = lightColorScheme(
    primary = ModeGfiPrimary,
    onPrimary = ModeGfiOnPrimary,
    secondary = BrandPurple40,
    tertiary = BrandBlue40,
    secondaryContainer = ModeGfiSecondaryContainer,
    onSecondaryContainer = ModeGfiOnSecondaryContainer,
    background = ModeGfiBackground,
    surface = ModeGfiSurface,
)

private val HwColorScheme = lightColorScheme(
    primary = ModeHwPrimary,
    onPrimary = ModeHwOnPrimary,
    secondary = BrandGreen40,
    tertiary = BrandBlue40,
    secondaryContainer = ModeHwSecondaryContainer,
    onSecondaryContainer = ModeHwOnSecondaryContainer,
    background = ModeHwBackground,
    surface = ModeHwSurface,
)

/**
 * Maps the active Issue Mode to its pastel ColorScheme — "Now you are
 * in GFI, light purple pastel" / "Now you are in HW, light green
 * pastel," exactly as asked for.
 *
 * Deliberately a `when` with NO `else` branch: IssueLabelFilter is a
 * sealed-by-being-an-enum type, so Kotlin requires every entry to be
 * covered here or this won't compile. That's intentional, not an
 * oversight to tidy up later — it means adding a third or fourth Mode
 * (the explicit "not limited to 2 or 3 or 4" goal this was built
 * around) can't silently ship without also deciding that mode's
 * colors; the compiler forces the question rather than letting a new
 * mode quietly fall back to someone else's palette.
 *
 * LIGHT-ONLY FOR NOW — worth being direct about the real gap this
 * leaves: dark mode doesn't exist anywhere in the applied codebase
 * yet (the standalone dark-mode CM drafted earlier was deliberately
 * shelved rather than applied, specifically BECAUSE Mode's palette
 * design didn't exist yet to combine it with — see that CM's own
 * discussion). Now that this file exists, dark mode becomes a real,
 * well-defined follow-up: each mode would need a SECOND, dark-variant
 * ColorScheme here, and Theme.kt's selection logic would extend from
 * "which mode" to "which mode, crossed with light-or-dark." Until
 * that follow-up lands, entering a mode always shows its light pastel
 * scheme regardless of system dark-mode setting — a real, visible
 * simplification, not a bug, but one worth knowing about rather than
 * discovering by surprise.
 */
fun IssueLabelFilter.colorScheme(): ColorScheme = when (this) {
    IssueLabelFilter.GOOD_FIRST_ISSUE -> GfiColorScheme
    IssueLabelFilter.HELP_WANTED -> HwColorScheme
}
