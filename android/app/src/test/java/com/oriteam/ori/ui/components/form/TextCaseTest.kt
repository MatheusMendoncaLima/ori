package com.oriteam.ori.ui.components.form

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test

class TextCaseTest {

    @Test
    fun `uppercase transformation changes only what is displayed`() {
        val typed = AnnotatedString("abC1")

        val transformed = UppercaseVisualTransformation.filter(typed)

        assertEquals("ABC1", transformed.text.text)
        assertEquals("abC1", typed.text)
    }

    @Test
    fun `uppercase transformation keeps the text length so the cursor stays valid`() {
        // "ß".uppercase() is "SS" (two characters); per-character conversion must keep one.
        val transformed = UppercaseVisualTransformation.filter(AnnotatedString("straße"))

        assertEquals(6, transformed.text.length)
        assertEquals(3, transformed.offsetMapping.originalToTransformed(3))
    }
}
