package com.oriteam.ori.ui.app.screens.aac

import androidx.compose.runtime.Immutable
import com.oriteam.ori.domain.aac.AacSymbol
import com.oriteam.ori.domain.aac.MessagePart
import com.oriteam.ori.domain.aac.SymbolImage
import com.oriteam.ori.domain.photos.PhotoStore

/** Picture of a symbol, ready to draw. */
@Immutable
sealed interface SymbolImageUi {
    data class Emoji(val value: String) : SymbolImageUi

    /** [path] is null when the photo is not on this device (added on another one). */
    data class Photo(val path: String?) : SymbolImageUi
}

/** A symbol of an AAC (Comunicação Aumentativa e Alternativa) board, or a piece of a message. */
@Immutable
data class AacSymbolUiModel(
    val id: String,
    val label: String,
    val image: SymbolImageUi? = null,
    /** What is said; shown in the organizer so the person knows what a tap will say. */
    val spokenText: String = label,
    /** In a message: the word the grammar adds before this piece ("de" in "gosto de você"). */
    val connector: String? = null
)

fun SymbolImage.toUi(photoStore: PhotoStore): SymbolImageUi = when (this) {
    is SymbolImage.Emoji -> SymbolImageUi.Emoji(value)
    is SymbolImage.DevicePhoto -> SymbolImageUi.Photo(photoStore.pathOf(photo))
}

fun AacSymbol.toUiModel(photoStore: PhotoStore) = AacSymbolUiModel(
    id = id,
    label = label,
    image = image?.toUi(photoStore),
    spokenText = spokenText
)

/** Message pieces have no id of their own; the index makes each one unique in the bar. */
fun MessagePart.toUiModel(index: Int, photoStore: PhotoStore) = AacSymbolUiModel(
    id = "part-$index",
    label = label,
    image = image?.toUi(photoStore),
    spokenText = spokenText
)
