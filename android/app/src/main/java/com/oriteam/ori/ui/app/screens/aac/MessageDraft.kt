package com.oriteam.ori.ui.app.screens.aac

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import com.oriteam.ori.domain.aac.AacMessage
import com.oriteam.ori.domain.aac.MessagePart
import com.oriteam.ori.domain.aac.SymbolImage
import com.oriteam.ori.domain.aac.storedKey
import com.oriteam.ori.domain.aac.wordClassFromKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val MESSAGE_KEY = "aac_message"
private const val LABEL = "label"
private const val SPOKEN = "spoken"
private const val EMOJI = "emoji"
private const val PHOTO = "photo"
private const val WORD_CLASS = "word_class"

/**
 * The message being built. Each change is kept in the [SavedStateHandle], so it survives rotation and
 * the system closing the app in the background. It is never written to Firestore unless the person
 * saves it: an unsent message is a draft and stays on the device.
 */
internal class MessageDraft(private val handle: SavedStateHandle) {
    private val current = MutableStateFlow(restore())
    val message: StateFlow<AacMessage> = current.asStateFlow()

    fun update(transform: (AacMessage) -> AacMessage) {
        val updated = transform(current.value)
        save(updated)
        current.value = updated
    }

    private fun restore(): AacMessage {
        val stored = handle.get<ArrayList<Bundle>>(MESSAGE_KEY) ?: return AacMessage()
        return AacMessage(stored.mapNotNull(::partFrom))
    }

    private fun save(message: AacMessage) {
        handle[MESSAGE_KEY] = ArrayList(message.parts.map(::bundleOf))
    }

    private fun bundleOf(part: MessagePart): Bundle = bundleOf(
        LABEL to part.label,
        SPOKEN to part.spokenText,
        EMOJI to (part.image as? SymbolImage.Emoji)?.value,
        PHOTO to (part.image as? SymbolImage.DevicePhoto)?.fileName,
        WORD_CLASS to part.wordClass?.storedKey
    )

    private fun partFrom(bundle: Bundle): MessagePart? {
        val label = bundle.getString(LABEL) ?: return null
        val image = bundle.getString(EMOJI)?.let(SymbolImage::Emoji)
            ?: bundle.getString(PHOTO)?.let(SymbolImage::DevicePhoto)
        return MessagePart(
            label = label,
            spokenText = bundle.getString(SPOKEN) ?: label,
            image = image,
            wordClass = wordClassFromKey(bundle.getString(WORD_CLASS))
        )
    }
}
