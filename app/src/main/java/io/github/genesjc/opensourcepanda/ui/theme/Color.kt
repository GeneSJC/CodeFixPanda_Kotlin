package io.github.genesjc.opensourcepanda.ui.theme

import androidx.compose.ui.graphics.Color

// Baseline scheme (no Issue Mode chosen): the brand orange's deeper shades on a warm white.
// Replaces the Android Studio template's generic Purple80/Purple40 defaults.

// Light scheme
val BrandContainerLight = Color(0xFFFFDCC9)
val OnBrandContainerLight = Color(0xFF4A1A14)
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
val BrandBlue = Color(0xFF1F4B8E) // a lighter step of the navy, the blue that sits well beside the orange
val BrandBlueContainerLight = Color(0xFFD8E4F5)
val BrandMint = Color(0xFF6FD6A5) // the panda's scrubs as drawn, light: for tints and the artwork, not for text
val BrandSeafoam = Color(0xFF187A5E) // the scrubs' color deepened for the UI: white text on it passes contrast
val BrandSeafoamContainerLight = Color(0xFFDDF5EA)
val OnBrandSeafoamContainerLight = Color(0xFF0B3B2D)
val BrandOutlineBlack = Color(0xFF08080C) // the glyph's outline on the orange
val BrandSoftWhite = Color(0xFFFFECE0) // secondary text on the orange

// The soft tangerine for the content of the screens under the brand title bar: a pale gradient with the glyph
// in a deeper tangerine behind it, so the orange stays the title bar's color and the content stays readable.
val TangerineLight = Color(0xFFFFE2CC)
val TangerineMid = Color(0xFFFFCDAA)
val TangerineGlyph = Color(0xFFF09664)

// Issue Mode schemes — the brand blue and a deeper shade of the brand orange, which sit well together: a mode's
// colors have to comfortably BE the background/surface across a whole screen, so each pairs a strong primary
// (white text on it passes contrast) with a very light tint of the same hue. Beginner-friendly is the blue,
// projects asking for help the orange. Light-only for now — see ModeColorSchemes.kt for why dark variants
// aren't included yet. They reuse the brand constants above, so this block has to stay below them.

// Beginner-friendly issues — the blue
val ModeGfiBackground = Color(0xFFEEF3FB)
val ModeGfiSurface = Color(0xFFFFFFFF)
val ModeGfiPrimary = BrandBlue
val ModeGfiOnPrimary = Color(0xFFFFFFFF)
val ModeGfiSecondaryContainer = BrandBlueContainerLight
val ModeGfiOnSecondaryContainer = BrandNavy

// Projects asking for help — the orange
val ModeHwBackground = Color(0xFFFFF2E9)
val ModeHwSurface = Color(0xFFFFFFFF)
val ModeHwPrimary = BrandOrangeMid
val ModeHwOnPrimary = Color(0xFFFFFFFF)
val ModeHwSecondaryContainer = BrandContainerLight
val ModeHwOnSecondaryContainer = OnBrandContainerLight

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
