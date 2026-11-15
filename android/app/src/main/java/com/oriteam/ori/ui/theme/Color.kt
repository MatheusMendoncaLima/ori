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
val OriDecorationArcThick = Color(0xFFE1EFF8)
val OriDecorationArcThin = Color(0xFFD2E6F5)
val OriButtonCyan = Color(0xFF0082A3)
val OriSubtleText = Color(0xFF5A7186)
val OriFieldPlaceholder = Color(0xFF5C7387)
val OriCardBorder = Color(0xFFE5F0FA)

/**
 * The edge of a control: a text field, a choice, the circle that marks an activity as done, a page dot. At least 3:1
 * on the background and on white, the least for a control to be seen (WCAG 1.4.11); the light blue-gray of the first
 * screens (#C3D5E3) gave 1.5:1, and a white field on the light background almost disappeared.
 */
val OriControlBorder = Color(0xFF7A8C9C)
val OriToggleBorder = Color(0xFFE0ECF5)
val OriSwitchTrackInactive = Color(0xFFD0E3EF)
val OriErrorText = Color(0xFFD32F2F)

/**
 * The badge of an icon (how many notifications an app has), as on One UI: the same red in the light and the dark
 * theme, with white writing (contrast 5:1), so it reads the same on any wallpaper.
 */
val OriBadge = OriErrorText
val OnOriBadge = Color.White

/** The writing on an app's tile ("Máximo"), whose color is a dark, calm tone of the app's own. */
val OnOriTile = Color.White
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
    // A shade darker than the slogan blue, so secondary text keeps AA contrast also on the soft colors of rows,
    // symbols and activities (the slogan blue gave 4.1 to 4.4 on them).
    onSurfaceVariant = Color(0xFF546E85),
    surfaceContainerLowest = OriSurface,
    surfaceContainerLow = OriSurface,
    surfaceContainer = OriSurface,
    surfaceContainerHigh = OriBackground,
    surfaceContainerHighest = OriDecorationArcThick,
    // `outline` is the edge of what can be touched (3:1); `outlineVariant`, only decoration (cards, lines).
    outline = OriControlBorder,
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

/**
 * Inside the app (after login) the main color is the brand teal of the wireframe ("Falar", "+ Adicionar atividade"),
 * the same as the selected tabs, the chosen day and the action pill: one color to choose and to act, instead of the
 * cyan and the teal side by side. On the dark theme it is the light teal, readable as text and as a line. The start,
 * login and sign-up screens keep the cyan of the wireframe's start screen.
 */
internal val OriLightAppColorScheme = OriLightColorScheme.copy(primary = Color(0xFF0F6E72), onPrimary = OriSurface)
internal val OriDarkAppColorScheme = OriDarkColorScheme.copy(primary = Color(0xFF62C4BE), onPrimary = Color(0xFF0B2E2D))

/** A soft background and the stronger color that marks it, used to tell items apart. */
@Immutable
data class OriAccent(val container: Color, val mark: Color)

/**
 * The kind of a word on a communication board, as in the color keys of those boards: people in yellow, actions in
 * green, feelings in blue, answers and courtesy ("sim", "não", "por favor") in pink. The key is explained in the
 * communication preferences.
 */
@Immutable
data class OriWordColors(val person: OriAccent, val action: OriAccent, val feeling: OriAccent, val social: OriAccent)

/**
 * One color per page of Configurações, as in the wireframe: accessibility in mint, theme in blue, communication in
 * lilac (its area), screen in yellow, privacy in pink, about in gray.
 */
@Immutable
data class OriSettingsColors(
    val accessibility: OriAccent,
    val theme: OriAccent,
    val communication: OriAccent,
    val screen: OriAccent,
    val privacy: OriAccent,
    val about: OriAccent
)

/** The part of the day of an activity: morning in yellow, afternoon in orange, night in lilac, no time in green. */
@Immutable
data class OriDayPartColors(
    val morning: OriAccent,
    val afternoon: OriAccent,
    val night: OriAccent,
    val anyTime: OriAccent
)

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
    /** One per area of the app ([OriAreaAccents]): the menus and what belongs to an area. */
    val accents: List<OriAccent>,
    val words: OriWordColors,
    val dayParts: OriDayPartColors,
    val settings: OriSettingsColors
)

// The soft colors of the app. Each set above gives them a meaning (an area, a kind of word, a part of the day);
// a hue may mean different things in different sets, but never two things on the same screen.
private val LightMint = OriAccent(Color(0xFFE6F6F1), Color(0xFF3FB39B))
private val LightOrange = OriAccent(Color(0xFFFFEEE6), Color(0xFFE08A5B))
private val LightYellow = OriAccent(Color(0xFFFFF7DC), Color(0xFFD9B23C))
private val LightBlue = OriAccent(Color(0xFFE6F1FC), Color(0xFF5B9BD9))
private val LightLilac = OriAccent(Color(0xFFF0EBFB), Color(0xFF9B83D6))
private val LightGreen = OriAccent(Color(0xFFEAF8EC), Color(0xFF62B56D))
private val LightPink = OriAccent(Color(0xFFFCE9F1), Color(0xFFD86A95))
private val LightNeutral = OriAccent(Color(0xFFEDF2F6), Color(0xFF8A9BAB))

// On dark backgrounds the same hues, deep but saturated enough to be told apart from the card they sit on (the
// first ones, almost gray, made the colors look lost). OriThemeRulesTest keeps the text on them at AA contrast.
private val DarkMint = OriAccent(Color(0xFF1B463F), Color(0xFF5FBFAB))
private val DarkOrange = OriAccent(Color(0xFF4A3226), Color(0xFFDDA188))
private val DarkYellow = OriAccent(Color(0xFF453C1E), Color(0xFFD2B76C))
private val DarkBlue = OriAccent(Color(0xFF1D3C5A), Color(0xFF7FAEDB))
private val DarkLilac = OriAccent(Color(0xFF35305C), Color(0xFFA89BDB))
private val DarkGreen = OriAccent(Color(0xFF20442D), Color(0xFF84C49A))
private val DarkPink = OriAccent(Color(0xFF4A2638), Color(0xFFE39AB8))
private val DarkNeutral = OriAccent(Color(0xFF26384A), Color(0xFF9CAFBF))

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
    accents = listOf(LightMint, LightOrange, LightYellow, LightBlue, LightLilac, LightGreen),
    words = OriWordColors(person = LightYellow, action = LightMint, feeling = LightBlue, social = LightPink),
    dayParts = OriDayPartColors(
        morning = LightYellow,
        afternoon = LightOrange,
        night = LightLilac,
        anyTime = LightMint
    ),
    settings = OriSettingsColors(LightMint, LightBlue, LightLilac, LightYellow, LightPink, LightNeutral)
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
    accents = listOf(DarkMint, DarkOrange, DarkYellow, DarkBlue, DarkLilac, DarkGreen),
    words = OriWordColors(person = DarkYellow, action = DarkMint, feeling = DarkBlue, social = DarkPink),
    dayParts = OriDayPartColors(
        morning = DarkYellow,
        afternoon = DarkOrange,
        night = DarkLilac,
        anyTime = DarkMint
    ),
    settings = OriSettingsColors(DarkMint, DarkBlue, DarkLilac, DarkYellow, DarkPink, DarkNeutral)
)

val LocalOriColors = staticCompositionLocalOf { OriLightExtraColors }
