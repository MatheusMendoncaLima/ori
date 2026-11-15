package com.oriteam.ori.ui.app.screens.aac

import androidx.annotation.StringRes
import com.oriteam.ori.R
import com.oriteam.ori.domain.aac.moved

/**
 * A sentence that comes with the app and the picture that helps to find it without reading. [id] names it in the
 * order the person chose ([com.oriteam.ori.domain.preferences.AacPreferences.readyPhraseOrder]): it never changes,
 * unlike the number of its text, which changes from one build to the next.
 */
data class ReadyPhrase(val id: String, @StringRes val textRes: Int, val emoji: String)

/** The most urgent first, because the list is also what opens first. All avoid grammatical gender. */
val readyPhrases = listOf(
    ReadyPhrase("yes", R.string.aac_phrase_yes, "👍"),
    ReadyPhrase("no", R.string.aac_phrase_no, "👎"),
    ReadyPhrase("help", R.string.aac_phrase_help, "🆘"),
    ReadyPhrase("pain", R.string.aac_phrase_pain, "🤕"),
    ReadyPhrase("water", R.string.aac_phrase_water, "🥛"),
    ReadyPhrase("hungry", R.string.aac_phrase_hungry, "🍽️"),
    ReadyPhrase("bathroom", R.string.aac_phrase_bathroom, "🚻"),
    ReadyPhrase("sleepy", R.string.aac_phrase_sleepy, "😴"),
    ReadyPhrase("cold", R.string.aac_phrase_cold, "🥶"),
    ReadyPhrase("hot", R.string.aac_phrase_hot, "🥵"),
    ReadyPhrase("rest", R.string.aac_phrase_rest, "🛌"),
    ReadyPhrase("please", R.string.aac_phrase_please, "🙏"),
    ReadyPhrase("not_understood", R.string.aac_phrase_not_understood, "🤔"),
    ReadyPhrase("repeat", R.string.aac_phrase_repeat, "🔁"),
    ReadyPhrase("wait", R.string.aac_phrase_wait, "✋"),
    ReadyPhrase("good_morning", R.string.aac_phrase_good_morning, "☀️")
)

/**
 * The ready-made phrases in the [order] chosen (their ids). An id no longer in the app is skipped; a phrase not in the
 * order (one the app brought after the order was chosen) goes at the end, in the app's order.
 */
fun readyPhrasesIn(order: List<String>): List<ReadyPhrase> {
    val byId = readyPhrases.associateBy { it.id }
    val chosen = order.distinct().mapNotNull(byId::get)
    return chosen + readyPhrases.filter { it !in chosen }
}

/** The order of every ready-made phrase after [id] goes to [toIndex]; null when nothing moves. */
fun readyOrderAfterMoving(order: List<String>, id: String, toIndex: Int): List<String>? {
    val ids = readyPhrasesIn(order).map { it.id }
    val moved = ids.moved(ids.indexOf(id), toIndex)
    return moved.takeIf { it != ids }
}
