package com.oriteam.ori.domain.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceSettingsTest {

    @Test
    fun `a style changes tone and speed and keeps the chosen voice`() {
        val settings = VoiceSettings(voiceId = "pt-br-x-ptd-local").with(VoicePreset.CHILD)

        assertEquals(VoiceSettings("pt-br-x-ptd-local", pitch = 1.6f, rate = 1.1f), settings)
        assertEquals(VoicePreset.CHILD, settings.preset)
    }

    @Test
    fun `a tone or speed between the styles is no style`() {
        assertEquals(VoicePreset.NATURAL, VoiceSettings().preset)
        assertNull(VoiceSettings(pitch = 1.05f).preset)
    }

    @Test
    fun `values beyond what engines understand come back inside the range`() {
        val normalized = VoiceSettings(pitch = 9f, rate = 0.1f).normalized()

        assertEquals(MAX_VOICE_PITCH, normalized.pitch)
        assertEquals(MIN_VOICE_RATE, normalized.rate)
    }
}
