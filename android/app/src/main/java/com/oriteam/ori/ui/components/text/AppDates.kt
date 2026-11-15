package com.oriteam.ori.ui.components.text

import android.provider.Settings
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.oriteam.ori.domain.common.DefaultAppLocale
import com.oriteam.ori.domain.common.appLocaleFor
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The language of dates and day names on screen. `MainActivity` provides the device's
 * ([rememberDeviceAppLocale]); tests and previews get the default, the language of `values/strings.xml`.
 */
val LocalAppLocale = staticCompositionLocalOf { DefaultAppLocale }

/** The app's language for the device's languages, chosen again when the person changes them. */
@Composable
fun rememberDeviceAppLocale(): Locale {
    val locales = LocalConfiguration.current.locales
    return remember(locales) { appLocaleFor(List(locales.size()) { locales[it] }) }
}

/**
 * A date format from a skeleton: the fields, without order or punctuation ("EEEEdMMMM" is weekday, day and
 * month). The language chooses the order and the words: "sábado, 7 de novembro" in Portuguese, "Saturday,
 * November 7" in English. A pattern written by hand ("d 'de' MMMM") is right in one language only.
 */
@Composable
fun rememberDateFormat(skeleton: String): DateTimeFormatter {
    val locale = LocalAppLocale.current
    return remember(skeleton, locale) { dateFormatFor(skeleton, locale) }
}

internal fun dateFormatFor(skeleton: String, locale: Locale): DateTimeFormatter =
    DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)

/**
 * The time of an activity: "08:30" in Portuguese, "8:30 AM" in American English. A 12- or 24-hour choice made in
 * the device's settings wins over the language's habit.
 */
@Composable
fun rememberTimeFormat(): DateTimeFormatter {
    val locale = LocalAppLocale.current
    val context = LocalContext.current
    val systemChoice = remember(context) {
        Settings.System.getString(context.contentResolver, Settings.System.TIME_12_24)
    }
    return remember(locale, systemChoice) { timeFormatFor(locale, systemChoice) }
}

/** [systemChoice] is the device's setting: "12", "24", or null when the person never chose. */
internal fun timeFormatFor(locale: Locale, systemChoice: String?): DateTimeFormatter {
    val skeleton = when (systemChoice) {
        "12" -> "hm"
        "24" -> "Hm"
        // "j" is the hour as the language writes it: 24 hours in Portuguese, 12 with AM/PM in American English.
        else -> "jm"
    }
    return dateFormatFor(skeleton, locale)
}

/** The skeletons the app uses, so the same date reads the same way on every screen. */
object DateSkeletons {
    /** "sábado, 7 de novembro" · "Saturday, November 7". */
    const val WEEKDAY_DAY_MONTH = "EEEEdMMMM"

    /** "novembro de 2026" · "November 2026". */
    const val MONTH_YEAR = "MMMMy"

    /** "07/11" · "11/07": day and month in numbers. */
    const val DAY_MONTH_NUMBERS = "ddMM"
}
