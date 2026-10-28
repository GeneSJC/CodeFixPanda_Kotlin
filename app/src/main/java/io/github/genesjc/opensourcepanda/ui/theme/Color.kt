package io.github.genesjc.opensourcepanda.ui.theme

import androidx.compose.ui.graphics.Color

// Brand palette — a teal/green primary paired with a purple secondary that
// echoes the beginner-friendly label color already used in LabelChip.
// Replaces the Android Studio template's generic Purple80/Purple40 defaults.

// Light scheme
val BrandGreen40 = Color(0xFF0F6E56)
val BrandPurple40 = Color(0xFF534AB7)
val BrandBlue40 = Color(0xFF0B5FA5)
val GreenContainerLight = Color(0xFFD9F2EA)
val OnGreenContainerLight = Color(0xFF0B4A3A)
val BackgroundLight = Color(0xFFFAF9F7)
val SurfaceLight = Color(0xFFFFFFFF)

// Dark scheme
val BrandGreen80 = Color(0xFF5DCAA5)
val BrandPurple80 = Color(0xFFAFA9EC)
val BrandBlue80 = Color(0xFF9FC9F0)
val GreenContainerDark = Color(0xFF1E3D34)
val OnGreenContainerDark = Color(0xFFB6E7D6)
val BackgroundDark = Color(0xFF14140F)
val SurfaceDark = Color(0xFF1E1E1A)

// Issue Mode pastel schemes (v0.41) — deliberately their own palette,
// not a reuse of BrandPurple/BrandGreen above at full strength. Those
// exist to accent small elements (a label chip, a secondary color) on
// an otherwise neutral background; Mode's colors need to comfortably
// BE the background/surface across an entire screen without turning
// harsh or fatiguing, so they're a good deal lighter and softer than
// the brand accents they're loosely echoing. Light-only for now — see
// ModeColorSchemes.kt for why dark variants aren't included yet.

// Good First Issue — light purple pastel
val ModeGfiBackground = Color(0xFFF5F2FC)
val ModeGfiSurface = Color(0xFFFFFFFF)
val ModeGfiPrimary = Color(0xFF6E5FD1)
val ModeGfiOnPrimary = Color(0xFFFFFFFF)
val ModeGfiSecondaryContainer = Color(0xFFE6E0F8)
val ModeGfiOnSecondaryContainer = Color(0xFF2E2566)

// Help Wanted — light green pastel
val ModeHwBackground = Color(0xFFEFFAF1)
val ModeHwSurface = Color(0xFFFFFFFF)
val ModeHwPrimary = Color(0xFF2F9E5C)
val ModeHwOnPrimary = Color(0xFFFFFFFF)
val ModeHwSecondaryContainer = Color(0xFFD7F2DF)
val ModeHwOnSecondaryContainer = Color(0xFF0B4A24)

// Hamburger-menu section backgrounds (TopReposScreen's drawer) —
// deliberately their own fixed pastel set, NOT derived from the active
// Issue Mode's palette above: these are structural UI chrome (grouping
// related menu items), not something that should repaint itself as Mode
// changes the way the rest of the screen does.
val DrawerSectionRepoManagement = Color(0xFFE9F1FB) // light blue
val DrawerSectionFindIssues = Color(0xFFFDF1E0) // light amber
val DrawerSectionSandbox = Color(0xFFEFEFEF) // light neutral gray
