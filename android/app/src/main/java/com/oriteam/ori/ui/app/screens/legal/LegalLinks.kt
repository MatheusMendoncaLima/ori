package com.oriteam.ori.ui.app.screens.legal

import com.oriteam.ori.domain.common.appLanguageTag
import java.util.Locale

/**
 * The public pages of the legal documents (web/, published with Firebase Hosting), in the app's language. The
 * same addresses go in the store listing.
 */
object LegalLinks {
    private const val SITE = "https://oriteam-ori.web.app"
    private val languages = setOf("pt", "en", "es")

    private fun language(): String =
        Locale.forLanguageTag(appLanguageTag).language.takeIf { it in languages } ?: "pt"

    fun privacyPolicy() = "$SITE/${language()}/privacy/"

    fun termsOfUse() = "$SITE/${language()}/terms/"

    fun accountDeletion() = "$SITE/${language()}/delete-account/"
}
