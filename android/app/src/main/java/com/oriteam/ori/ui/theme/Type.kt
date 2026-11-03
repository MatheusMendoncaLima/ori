package com.oriteam.ori.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.oriteam.ori.R

private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val NunitoFont = GoogleFont("Nunito")
val NunitoSansFont = GoogleFont("Nunito Sans")

val NunitoFontFamily = FontFamily(
    Font(googleFont = NunitoFont, fontProvider = fontProvider),
    Font(googleFont = NunitoFont, fontProvider = fontProvider, weight = FontWeight.Bold),
    Font(googleFont = NunitoFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = NunitoFont, fontProvider = fontProvider, weight = FontWeight.ExtraBold)
)

val NunitoSansFontFamily = FontFamily(
    Font(googleFont = NunitoSansFont, fontProvider = fontProvider),
    Font(googleFont = NunitoSansFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = NunitoSansFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = NunitoSansFont, fontProvider = fontProvider, weight = FontWeight.Bold)
)

private fun nunitoSans(weight: FontWeight, size: Int, lineHeight: Int? = null) = TextStyle(
    fontFamily = NunitoSansFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = (lineHeight ?: (size * LINE_HEIGHT_RATIO).toInt()).sp
)

/** Line height relative to font size; keeps multi-line text readable when it wraps or is enlarged. */
private const val LINE_HEIGHT_RATIO = 1.4f

/**
 * Text roles used by every screen. Sizes are the ones of the original ORI screens, so
 * migrating a `Text` to a role keeps its appearance. All sizes are in `sp` and follow
 * the system font scale.
 */
val Typography = Typography(
    // Brand name on the onboarding page.
    displayLarge = TextStyle(fontFamily = NunitoFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 82.sp),
    // Brand name in the header of form screens.
    displaySmall = TextStyle(fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp),
    // Screen titles: primary user (large) and caregiver (smaller, task-oriented).
    headlineLarge = nunitoSans(FontWeight.Bold, size = 32),
    headlineMedium = nunitoSans(FontWeight.Bold, size = 26),
    headlineSmall = nunitoSans(FontWeight.Bold, size = 21),
    titleLarge = nunitoSans(FontWeight.Bold, size = 19),
    titleMedium = nunitoSans(FontWeight.Bold, size = 17),
    titleSmall = nunitoSans(FontWeight.Bold, size = 15),
    bodyLarge = nunitoSans(FontWeight.Medium, size = 15),
    bodyMedium = nunitoSans(FontWeight.Normal, size = 14),
    bodySmall = nunitoSans(FontWeight.Normal, size = 13),
    // Buttons.
    labelLarge = nunitoSans(FontWeight.Bold, size = 16),
    // Field labels and text links.
    labelMedium = nunitoSans(FontWeight.Bold, size = 14),
    // Feedback messages, slogan and small badges.
    labelSmall = nunitoSans(FontWeight.SemiBold, size = 13)
)
