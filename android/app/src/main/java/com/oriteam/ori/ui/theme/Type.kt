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

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp
    ),
    titleLarge = TextStyle(
        fontFamily = NunitoSansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = NunitoSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = NunitoSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    )
)
