package com.oriteam.ori.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ORI brand palette. These values come from the original screens and define the visual identity.
val OriBackground = Color(0xFFF2FAFE)
val OriDarkBlue = Color(0xFF102A45)
val OriSloganBlue = Color(0xFF5C768D)
val OriIndicatorInactive = Color(0xFFC3D5E3)
val OriDecorationArcThick = Color(0xFFE1EFF8)
val OriDecorationArcThin = Color(0xFFD2E6F5)
val OriButtonCyan = Color(0xFF0082A3)
val OriSubtleText = Color(0xFF5A7186)
val OriFieldPlaceholder = Color(0xFF5C7387)
val OriCardBorder = Color(0xFFE5F0FA)
val OriToggleBorder = Color(0xFFE0ECF5)
val OriSwitchTrackInactive = Color(0xFFD0E3EF)
val OriErrorText = Color(0xFFD32F2F)
val OriSuccessText = Color(0xFF2E7D32)
val OriSurface = Color.White

/**
 * Maps the brand palette to Material 3 roles, so Material components (navigation bar, rail,
 * dialogs, switches) use ORI colors without per-component overrides.
 */
internal val OriLightColorScheme = lightColorScheme(
    primary = OriButtonCyan,
    onPrimary = OriSurface,
    primaryContainer = OriDecorationArcThick,
    onPrimaryContainer = OriDarkBlue,
    secondary = OriSloganBlue,
    onSecondary = OriSurface,
    secondaryContainer = OriDecorationArcThick,
    onSecondaryContainer = OriDarkBlue,
    tertiary = OriSuccessText,
    onTertiary = OriSurface,
    background = OriBackground,
    onBackground = OriDarkBlue,
    surface = OriSurface,
    onSurface = OriDarkBlue,
    surfaceVariant = OriBackground,
    onSurfaceVariant = OriSloganBlue,
    surfaceContainerLowest = OriSurface,
    surfaceContainerLow = OriSurface,
    surfaceContainer = OriSurface,
    surfaceContainerHigh = OriBackground,
    surfaceContainerHighest = OriDecorationArcThick,
    outline = OriIndicatorInactive,
    outlineVariant = OriCardBorder,
    error = OriErrorText,
    onError = OriSurface
)

/**
 * The same brand on dark backgrounds: a deep version of the brand navy as the base, in steps that
 * separate the levels without borders, and calmer accents, so color marks what matters instead of
 * competing for attention. OriThemeRulesTest checks the contrast of each pair.
 */
internal val OriDarkColorScheme = darkColorScheme(
    primary = Color(0xFF5ABDD8),
    onPrimary = Color(0xFF00303D),
    primaryContainer = Color(0xFF1F3A4F),
    onPrimaryContainer = Color(0xFFE4EDF4),
    secondary = Color(0xFFA7B9C8),
    onSecondary = Color(0xFF102A45),
    secondaryContainer = Color(0xFF1F3A4F),
    onSecondaryContainer = Color(0xFFE4EDF4),
    tertiary = Color(0xFF86C79A),
    onTertiary = Color(0xFF0B2E1A),
    background = Color(0xFF0C1824),
    onBackground = Color(0xFFE4EDF4),
    surface = Color(0xFF142230),
    onSurface = Color(0xFFE4EDF4),
    surfaceVariant = Color(0xFF0C1824),
    onSurfaceVariant = Color(0xFFA7B9C8),
    surfaceContainerLowest = Color(0xFF0C1824),
    surfaceContainerLow = Color(0xFF142230),
    surfaceContainer = Color(0xFF142230),
    surfaceContainerHigh = Color(0xFF1C2E3F),
    surfaceContainerHighest = Color(0xFF243849),
    outline = Color(0xFF5E7488),
    outlineVariant = Color(0xFF283A4B),
    error = Color(0xFFF2A29B),
    onError = Color(0xFF3B0A05)
)

/** A soft background and the stronger color that marks it, used to tell items apart. */
@Immutable
data class OriAccent(val container: Color, val mark: Color)

/** Brand roles that Material 3 has no name for. */
@Immutable
data class OriExtraColors(
    val fieldPlaceholder: Color,
    val subtleText: Color,
    val toggleBorder: Color,
    val switchTrackInactive: Color,
    val arcThick: Color,
    val arcThin: Color,
    val errorText: Color,
    val successText: Color,
    /** Selected tabs, main actions and the profile header inside the app. */
    val strong: Color,
    val onStrong: Color,
    val strongSoft: Color,
    /** The same teal as a line or text directly on a background: [strong] is made to be filled. */
    val strongOnSurface: Color,
    val accents: List<OriAccent>
)

internal val OriLightExtraColors = OriExtraColors(
    fieldPlaceholder = OriFieldPlaceholder,
    subtleText = OriSubtleText,
    toggleBorder = OriToggleBorder,
    switchTrackInactive = OriSwitchTrackInactive,
    arcThick = OriDecorationArcThick,
    arcThin = OriDecorationArcThin,
    errorText = OriErrorText,
    successText = OriSuccessText,
    strong = Color(0xFF0F6E72),
    onStrong = OriSurface,
    strongSoft = Color(0xFF2C8080),
    strongOnSurface = Color(0xFF0F6E72),
    accents = listOf(
        OriAccent(Color(0xFFE6F6F1), Color(0xFF3FB39B)),
        OriAccent(Color(0xFFFFEEE6), Color(0xFFE08A5B)),
        OriAccent(Color(0xFFFFF7DC), Color(0xFFD9B23C)),
        OriAccent(Color(0xFFE6F1FC), Color(0xFF5B9BD9)),
        OriAccent(Color(0xFFF0EBFB), Color(0xFF9B83D6)),
        OriAccent(Color(0xFFEAF8EC), Color(0xFF62B56D))
    )
)

internal val OriDarkExtraColors = OriExtraColors(
    fieldPlaceholder = Color(0xFF8FA3B4),
    subtleText = Color(0xFF9CAFBF),
    toggleBorder = Color(0xFF283A4B),
    switchTrackInactive = Color(0xFF3A4F62),
    arcThick = Color(0xFF142230),
    arcThin = Color(0xFF1F3A4F),
    errorText = Color(0xFFF2A29B),
    successText = Color(0xFF86C79A),
    // The deep teal of the light theme with light text, instead of a bright block with dark text.
    strong = Color(0xFF12706F),
    onStrong = Color(0xFFF2FBFA),
    strongSoft = Color(0xFF0E5655),
    strongOnSurface = Color(0xFF62C4BE),
    // Same lightness and little saturation: they tell rows apart without looking like six brands.
    accents = listOf(
        OriAccent(Color(0xFF17302F), Color(0xFF5FBFAB)),
        OriAccent(Color(0xFF302A2B), Color(0xFFDDA188)),
        OriAccent(Color(0xFF2E2C25), Color(0xFFD2B76C)),
        OriAccent(Color(0xFF192D40), Color(0xFF7FAEDB)),
        OriAccent(Color(0xFF252A42), Color(0xFFA89BDB)),
        OriAccent(Color(0xFF1C3027), Color(0xFF84C49A))
    )
)

val LocalOriColors = staticCompositionLocalOf { OriLightExtraColors }
