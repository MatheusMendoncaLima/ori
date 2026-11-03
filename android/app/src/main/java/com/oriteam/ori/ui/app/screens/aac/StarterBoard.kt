package com.oriteam.ori.ui.app.screens.aac

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalResources
import com.oriteam.ori.R
import com.oriteam.ori.domain.aac.SymbolDraft
import com.oriteam.ori.domain.aac.SymbolImage

private data class StarterSymbol(@StringRes val label: Int, val emoji: String?)

/**
 * A small first vocabulary for people who prefer not to start empty. It is only a suggestion:
 * once created, every symbol can be edited, moved or deleted. Only words, never whole sentences, so
 * they combine into a sentence (see composeSentence, which adds "de" and "com" where they are
 * needed). Complete sentences are in "Frases prontas". Four rows, because a fifth would hide "dor"
 * below the fold on a common phone; "com" has no honest picture.
 */
private val starterSymbols = listOf(
    StarterSymbol(R.string.aac_starter_i, "🙋"),
    StarterSymbol(R.string.aac_starter_you, "🧑"),
    StarterSymbol(R.string.aac_starter_am, "🌙"),
    StarterSymbol(R.string.aac_starter_no, "🚫"),
    StarterSymbol(R.string.aac_starter_want, "👉"),
    StarterSymbol(R.string.aac_starter_need, "💡"),
    StarterSymbol(R.string.aac_starter_like, "❤️"),
    StarterSymbol(R.string.aac_starter_with, null),
    StarterSymbol(R.string.aac_starter_water, "🥛"),
    StarterSymbol(R.string.aac_starter_food, "🍎"),
    StarterSymbol(R.string.aac_starter_bathroom, "🚻"),
    StarterSymbol(R.string.aac_starter_help, "🆘"),
    StarterSymbol(R.string.aac_starter_pain, "🤕"),
    StarterSymbol(R.string.aac_starter_sleepy, "😴"),
    StarterSymbol(R.string.aac_starter_hungry, "🍽️"),
    StarterSymbol(R.string.aac_starter_cold, "🥶")
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
