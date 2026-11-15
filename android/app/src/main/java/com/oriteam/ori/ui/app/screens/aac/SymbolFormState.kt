package com.oriteam.ori.ui.app.screens.aac

import androidx.lifecycle.SavedStateHandle
import com.oriteam.ori.domain.aac.SymbolColor
import com.oriteam.ori.domain.aac.SymbolImage
import com.oriteam.ori.domain.aac.storedKey
import com.oriteam.ori.domain.aac.wordClassFromKey

private const val SYMBOL_ID = "symbol_editor_id"
private const val LABEL = "symbol_editor_label"
private const val PHRASE = "symbol_editor_phrase"
private const val EMOJI = "symbol_editor_emoji"
private const val PHOTO = "symbol_editor_photo"
private const val IS_NEW = "symbol_editor_is_new"
private const val ORIGINAL_PHOTO = "symbol_editor_original_photo"
private const val ORIGINAL_EMOJI = "symbol_editor_original_emoji"
private const val IMPORTED_PHOTOS = "symbol_editor_imported"
private const val OPENING = "symbol_editor_opening"
private const val COLOR = "symbol_editor_color"
private const val WORD_CLASS = "symbol_editor_word_class"
private val allKeys = listOf(
    SYMBOL_ID, LABEL, PHRASE, EMOJI, PHOTO, IS_NEW, ORIGINAL_PHOTO, ORIGINAL_EMOJI, IMPORTED_PHOTOS, OPENING, COLOR,
    WORD_CLASS
)

/**
 * Every field of the symbol form lives in the [SavedStateHandle], so a draft survives process
 * death. It also remembers photos imported by this form, to remove the ones that end up unused.
 */
internal class SymbolFormState(private val handle: SavedStateHandle) {

    val hasDraft: Boolean get() = handle.contains(SYMBOL_ID)
    val symbolId: String? get() = handle[SYMBOL_ID]

    /** Which opening of the form the draft belongs to (see `rememberFormOpening`). */
    val opening: String? get() = handle[OPENING]

    val originalImage: SymbolImage?
        get() = imageFrom(emoji = handle[ORIGINAL_EMOJI], photo = handle[ORIGINAL_PHOTO])

    fun start(opening: String, symbolId: String, originalImage: SymbolImage?) {
        handle[OPENING] = opening
        handle[SYMBOL_ID] = symbolId
        handle[ORIGINAL_EMOJI] = (originalImage as? SymbolImage.Emoji)?.value
        handle[ORIGINAL_PHOTO] = (originalImage as? SymbolImage.DevicePhoto)?.fileName
    }

    fun rememberImported(photo: SymbolImage.DevicePhoto) {
        handle[IMPORTED_PHOTOS] = ArrayList(importedPhotos().map { it.fileName } + photo.fileName)
    }

    fun importedPhotos(): List<SymbolImage.DevicePhoto> =
        handle.get<ArrayList<String>>(IMPORTED_PHOTOS).orEmpty().map(SymbolImage::DevicePhoto)

    fun save(state: SymbolEditorUiState) {
        handle[LABEL] = state.labelInput
        handle[PHRASE] = state.phraseInput
        handle[EMOJI] = (state.image as? SymbolImage.Emoji)?.value
        handle[PHOTO] = (state.image as? SymbolImage.DevicePhoto)?.fileName
        handle[IS_NEW] = state.isNew
        handle[COLOR] = state.color?.key
        handle[WORD_CLASS] = state.wordClass?.storedKey
    }

    fun restore(): SymbolEditorUiState {
        if (!hasDraft) return SymbolEditorUiState()
        return SymbolEditorUiState(
            labelInput = handle[LABEL] ?: "",
            phraseInput = handle[PHRASE] ?: "",
            image = imageFrom(emoji = handle[EMOJI], photo = handle[PHOTO]),
            isNew = handle[IS_NEW] ?: true,
            color = SymbolColor.fromKey(handle[COLOR]),
            wordClass = wordClassFromKey(handle[WORD_CLASS])
        )
    }

    fun clear() = allKeys.forEach { handle.remove<Any>(it) }

    private fun imageFrom(emoji: String?, photo: String?): SymbolImage? =
        emoji?.let(SymbolImage::Emoji) ?: photo?.let(SymbolImage::DevicePhoto)
}
