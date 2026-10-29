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

// App identity — the launcher icon's palette. These colors stand for the app itself (the splash, the
// front screens, the artwork), as opposed to the Issue Mode colors below, which stand for what the user
// is looking at. Keeping the two groups apart is what stops the palette growing a new color per screen.
val BrandOrangeTop = Color(0xFFCF6657)
val BrandOrangeMid = Color(0xFFBC524A)
val BrandOrangeBottom = Color(0xFFA5403E)
val BrandSun = Color(0xFFFFD94D) // the branch glyph
val BrandNavy = Color(0xFF0F2A52) // outlines and text on the sun and mint accents
val BrandMint = Color(0xFF6FD6A5) // the panda's scrubs
val BrandOutlineBlack = Color(0xFF08080C) // the glyph's outline on the orange
val BrandSoftWhite = Color(0xFFFFECE0) // secondary text on the orange

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

// The rate-limit gauge: a traffic-light scale (red is empty, green is full), the same on every screen.
val RateLimitRed = Color(0xFFE53935)
val RateLimitAmber = Color(0xFFFB8C00)
val RateLimitYellow = Color(0xFFFDD835)
val RateLimitGreen = Color(0xFF43A047)

// The front screens' (welcome and token guide) own palette, the icon's earlier blues.
val FrontDoorBlueDark = Color(0xFF0B1736)
val FrontDoorBlueMid = Color(0xFF1D4ED8)
val FrontDoorBlueLight = Color(0xFF2563EB)
val FrontDoorSkyBlue = Color(0xFF38BDF8)
val FrontDoorSoftWhite = Color(0xCCFFFFFF) // white at 80%, for secondary text and outlines on the gradient

// The older shape-stack splash variant draws three fixed colors of its own (it is not the launch splash).
val ShapeStackGreen = Color(0xFF0F6E56)
val ShapeStackPurple = Color(0xFF534AB7)
val ShapeStackBlue = Color(0xFF0B5FA5)
