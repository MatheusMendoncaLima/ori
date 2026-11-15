package com.oriteam.ori.testing

import java.util.Locale

/**
 * Runs [block] as on a device in [locale]: the app's language (`appLanguageTag`) comes from the JVM's default,
 * which the build sets to Portuguese for every test. The default comes back afterwards.
 */
inline fun <T> withDefaultLocale(locale: Locale, block: () -> T): T {
    val before = Locale.getDefault()
    Locale.setDefault(locale)
    try {
        return block()
    } finally {
        Locale.setDefault(before)
    }
}
