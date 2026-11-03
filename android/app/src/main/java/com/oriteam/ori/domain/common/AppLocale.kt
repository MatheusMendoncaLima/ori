package com.oriteam.ori.domain.common

import java.util.Locale

/**
 * The language of the app. Calendars, dates, the voice and the texts follow it, so translating the app
 * starts here, with a `values-xx/strings.xml`. The sentence grammar of the communication boards
 * (`domain/aac/SentencePiece.kt`) is Portuguese by nature: another language needs its own tables.
 */
val AppLocale: Locale = Locale.forLanguageTag("pt-BR")
