package io.github.genesjc.opensourcepanda.ui.theme

import androidx.compose.ui.graphics.Color

// Baseline scheme (no Issue Mode chosen): the blues the two modes are shades of, on a warm white.
// Replaces the Android Studio template's generic Purple80/Purple40 defaults.

// Light scheme
val BrandBlue40 = Color(0xFF0B5FA5)
val BlueContainerLight = Color(0xFFDCE8FD)
val OnBlueContainerLight = Color(0xFF0F2A66)
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

// Issue Mode schemes — two shades of blue, deliberately their own palette and not the brand orange: the
// orange is the app's identity (see above), while a mode's colors stand for what the user is looking at
// and have to comfortably BE the background/surface across an entire screen without turning harsh or
// fatiguing, so each pairs a strong primary (white text on it passes contrast) with a very light tint
// of the same blue. The two differ in depth: beginner-friendly is the lighter blue, projects asking for
// help the darker. Light-only for now — see ModeColorSchemes.kt for why dark variants aren't included yet.

// Beginner-friendly issues — the lighter blue
val ModeGfiBackground = Color(0xFFEEF4FF)
val ModeGfiSurface = Color(0xFFFFFFFF)
val ModeGfiPrimary = Color(0xFF2F6FE6)
val ModeGfiOnPrimary = Color(0xFFFFFFFF)
val ModeGfiSecondaryContainer = Color(0xFFDCE8FD)
val ModeGfiOnSecondaryContainer = Color(0xFF0F2A66)

// Projects asking for help — the darker blue
val ModeHwBackground = Color(0xFFE8EEFA)
val ModeHwSurface = Color(0xFFFFFFFF)
val ModeHwPrimary = Color(0xFF1D4ED8)
val ModeHwOnPrimary = Color(0xFFFFFFFF)
val ModeHwSecondaryContainer = Color(0xFFD3DEF7)
val ModeHwOnSecondaryContainer = Color(0xFF0B1E5E)

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
