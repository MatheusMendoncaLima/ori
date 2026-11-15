package com.oriteam.ori.ui.app.screens.aac

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalResources
import com.oriteam.ori.R
import com.oriteam.ori.domain.aac.StarterVocabulary
import com.oriteam.ori.domain.aac.SymbolDraft
import com.oriteam.ori.domain.aac.SymbolImage
import com.oriteam.ori.domain.common.DefaultAppLocale
import com.oriteam.ori.domain.common.TranslatedLanguages
import java.util.Locale

/** One word of the suggested board: [key] names it in every language (see [StarterVocabulary]). */
private data class StarterSymbol(val key: String, @StringRes val label: Int, val emoji: String?)

/**
 * A small first vocabulary for people who prefer not to start empty. It is only a suggestion:
 * once created, every symbol can be edited, moved or deleted. Only words, never whole sentences, so
 * they combine into a sentence (the SentenceGrammar of each language adds "de", "com", "to", "que"
 * where they are needed). Each language has its own words for the same places: Spanish says "tengo"
 * where Portuguese says "estou" ("tengo hambre"); StarterBoardLanguagesTest checks that each grammar
 * knows them. Complete sentences are in "Frases prontas". "com" has no honest picture.
 *
 * The most urgent words come first, in the four rows a common phone shows without scrolling ("dor" is in the
 * fourth); then answers and people, actions, and more feelings. Four rows of four on a phone are two rows of
 * eight on a tablet.
 */
private val starterSymbols = listOf(
    StarterSymbol("i", R.string.aac_starter_i, "🙋"),
    StarterSymbol("you", R.string.aac_starter_you, "🧑"),
    StarterSymbol("am", R.string.aac_starter_am, "🌙"),
    StarterSymbol("no", R.string.aac_starter_no, "🚫"),
    StarterSymbol("want", R.string.aac_starter_want, "👉"),
    StarterSymbol("need", R.string.aac_starter_need, "💡"),
    StarterSymbol("like", R.string.aac_starter_like, "❤️"),
    StarterSymbol("with", R.string.aac_starter_with, null),
    StarterSymbol("water", R.string.aac_starter_water, "🥛"),
    StarterSymbol("food", R.string.aac_starter_food, "🍎"),
    StarterSymbol("bathroom", R.string.aac_starter_bathroom, "🚻"),
    StarterSymbol("help", R.string.aac_starter_help, "🆘"),
    StarterSymbol("pain", R.string.aac_starter_pain, "🤕"),
    StarterSymbol("sleepy", R.string.aac_starter_sleepy, "😴"),
    StarterSymbol("hungry", R.string.aac_starter_hungry, "🍽️"),
    StarterSymbol("cold", R.string.aac_starter_cold, "🥶"),
    StarterSymbol("yes", R.string.aac_starter_yes, "👍"),
    StarterSymbol("more", R.string.aac_starter_more, "➕"),
    StarterSymbol("he_she", R.string.aac_starter_he_she, "🧍"),
    StarterSymbol("we", R.string.aac_starter_we, "👥"),
    StarterSymbol("go", R.string.aac_starter_go, "🚶"),
    StarterSymbol("eat", R.string.aac_starter_eat, "🥄"),
    StarterSymbol("drink", R.string.aac_starter_drink, "🥤"),
    StarterSymbol("sleep", R.string.aac_starter_sleep, "🛏️"),
    StarterSymbol("play", R.string.aac_starter_play, "🧸"),
    StarterSymbol("mom", R.string.aac_starter_mom, "👩"),
    StarterSymbol("dad", R.string.aac_starter_dad, "👨"),
    StarterSymbol("medicine", R.string.aac_starter_medicine, "💊"),
    StarterSymbol("hot", R.string.aac_starter_hot, "🥵"),
    StarterSymbol("scared", R.string.aac_starter_scared, "😨"),
    StarterSymbol("happy", R.string.aac_starter_happy, "😊"),
    StarterSymbol("sad", R.string.aac_starter_sad, "😢")
)

/** The starter board in the app language, ready to be stored. */
@Composable
fun rememberStarterBoard(): StarterBoardContent {
    val resources = LocalResources.current
    return remember(resources) {
        StarterBoardContent(
            name = resources.getString(R.string.aac_starter_board_name),
            symbols = starterSymbols.map {
                SymbolDraft(
                    label = resources.getString(it.label),
                    phrase = null,
                    image = it.emoji?.let(SymbolImage::Emoji)
                )
            }
        )
    }
}

/**
 * The suggested board in every language of the app, read from the texts of each one, so a board made in one
 * language can be shown in another (see [StarterVocabulary]).
 */
fun starterVocabularyOf(context: Context): StarterVocabulary {
    val texts = (TranslatedLanguages + DefaultAppLocale.language).associateWith { context.inLanguage(it).resources }
    return StarterVocabulary(
        names = texts.mapValues { (_, resources) -> resources.getString(R.string.aac_starter_board_name) },
        words = texts.mapValues { (_, resources) ->
            starterSymbols.associate { it.key to resources.getString(it.label) }
        },
        pictures = starterSymbols.associate { it.key to it.emoji }
    )
}

private fun Context.inLanguage(language: String): Context {
    val configuration = Configuration(resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) }
    return createConfigurationContext(configuration)
}
