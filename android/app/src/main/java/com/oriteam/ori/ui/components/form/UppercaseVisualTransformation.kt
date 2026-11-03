package com.oriteam.ori.ui.components.form

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Shows typed text in uppercase without changing the value kept in the screen state.
 *
 * Uppercasing the value itself would silently change passwords. Each character is converted on its own
 * so the text keeps its length and the cursor position ([OffsetMapping.Identity]) stays valid.
 */
object UppercaseVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val uppercaseText = buildString(text.length) {
            text.text.forEach { character -> append(character.uppercaseChar()) }
        }
        return TransformedText(AnnotatedString(uppercaseText), OffsetMapping.Identity)
    }
}
