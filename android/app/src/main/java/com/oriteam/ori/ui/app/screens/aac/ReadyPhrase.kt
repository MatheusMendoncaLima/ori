package com.oriteam.ori.ui.app.screens.aac

import androidx.annotation.StringRes
import com.oriteam.ori.R

/** A sentence that comes with the app and the picture that helps to find it without reading. */
data class ReadyPhrase(@StringRes val textRes: Int, val emoji: String)

/** The most urgent first, because the list is also what opens first. All avoid grammatical gender. */
val readyPhrases = listOf(
    ReadyPhrase(R.string.aac_phrase_yes, "👍"),
    ReadyPhrase(R.string.aac_phrase_no, "👎"),
    ReadyPhrase(R.string.aac_phrase_help, "🆘"),
    ReadyPhrase(R.string.aac_phrase_pain, "🤕"),
    ReadyPhrase(R.string.aac_phrase_water, "🥛"),
    ReadyPhrase(R.string.aac_phrase_hungry, "🍽️"),
    ReadyPhrase(R.string.aac_phrase_bathroom, "🚻"),
    ReadyPhrase(R.string.aac_phrase_sleepy, "😴"),
    ReadyPhrase(R.string.aac_phrase_cold, "🥶"),
    ReadyPhrase(R.string.aac_phrase_hot, "🥵"),
    ReadyPhrase(R.string.aac_phrase_rest, "🛌"),
    ReadyPhrase(R.string.aac_phrase_please, "🙏"),
    ReadyPhrase(R.string.aac_phrase_not_understood, "🤔"),
    ReadyPhrase(R.string.aac_phrase_repeat, "🔁"),
    ReadyPhrase(R.string.aac_phrase_wait, "✋"),
    ReadyPhrase(R.string.aac_phrase_good_morning, "☀️")
)
