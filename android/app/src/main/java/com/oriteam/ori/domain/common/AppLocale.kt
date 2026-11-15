package com.oriteam.ori.domain.common

import java.util.Locale

/**
 * The language of `values/strings.xml`, the reserve: used whenever none of the device's languages has texts.
 * It is Portuguese, so a device in French or German sees the app as it always did.
 */
val DefaultAppLocale: Locale = Locale.forLanguageTag("pt-BR")

/**
 * The languages with a `values-xx` folder of texts. Portuguese is here too: `values-pt` is a copy of `values`,
 * because Android does not know which language the plain `values` folder is in. Without the copy, a device with
 * "Portuguese, English", in that order, would find `values-en` first and show the app in English.
 * `AppLanguagesTest` compares this set with the folders and with `res/xml/locales_config.xml`, and the copy with
 * `values`, so a translation is added in all places.
 */
val TranslatedLanguages: Set<String> = setOf("pt", "en", "es")

/**
 * The language of dates, day names and the voice, chosen the way Android chooses the texts: the first of the
 * device's languages that has a translation, keeping the device's region (an "es-MX" device writes dates the
 * Mexican way), and otherwise the default. So the dates are never in another language than the words around
 * them. The per-app language of Android 13+ arrives here as the device's first language.
 */
fun appLocaleFor(deviceLocales: List<Locale>, translated: Set<String> = TranslatedLanguages): Locale =
    deviceLocales.firstOrNull { it.language in translated } ?: DefaultAppLocale

/**
 * The app's language as a language tag ("pt-BR"), recorded in what the person creates in it: a board, a saved
 * message. The voice and the grammar then follow what was created, not the language the app has later.
 */
val appLanguageTag: String
    get() = appLocaleFor(listOf(Locale.getDefault())).toLanguageTag()
