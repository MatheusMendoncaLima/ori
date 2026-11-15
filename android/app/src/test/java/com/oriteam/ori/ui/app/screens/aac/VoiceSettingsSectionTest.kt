package com.oriteam.ori.ui.app.screens.aac

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.domain.aac.SpeechState
import com.oriteam.ori.domain.aac.VoiceOption
import com.oriteam.ori.domain.preferences.VoicePreset
import com.oriteam.ori.domain.preferences.VoiceSound
import com.oriteam.ori.testing.FakeAacPreferencesRepository
import com.oriteam.ori.testing.FakeSpeechSynthesizer
import com.oriteam.ori.testing.OriTestWindow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VoiceSettingsSectionTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val preferences = FakeAacPreferencesRepository()

    private fun show(speech: FakeSpeechSynthesizer) {
        val viewModel = VoiceSettingsViewModel(preferences, speech)
        composeRule.setContent {
            OriTestWindow(widthDp = 360, heightDp = 1600) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    VoiceSettingsSection(viewModel)
                }
            }
        }
    }

    @Test
    fun `the voices are numbered, local ones first, and the one that needs internet says so`() {
        show(
            FakeSpeechSynthesizer().apply {
                voices.value = listOf(VoiceOption("pt-br-x-afs-local", false), VoiceOption("pt-br-x-ptd-network", true))
            }
        )

        // Closed, the row says which voice is in use; the list opens on request.
        composeRule.onNodeWithText("Em uso: Padrão do aparelho").assertExists()
        composeRule.onNodeWithText("Voz 1").assertDoesNotExist()
        composeRule.onNodeWithText("Ver as 2 vozes do aparelho").performScrollTo().performClick()

        composeRule.onNodeWithText("Padrão do aparelho").assertExists()
        composeRule.onNodeWithText("Voz 1").assertExists()
        composeRule.onNodeWithText("Voz 2 (precisa de internet)").assertExists()
    }

    @Test
    fun `the voice plays as media at first, and the ringtone is chosen with a tap`() {
        show(FakeSpeechSynthesizer())

        composeRule.onNodeWithText("Mídia").performScrollTo().assertIsSelected()
        composeRule.onNodeWithText("Toque").performScrollTo().performClick()

        assertEquals(VoiceSound.RING, preferences.preferences.value.voice.sound)
        composeRule.onNodeWithText("Toque").assertIsSelected()
    }

    @Test
    fun `tone and speed are read by name and value, and a style is chosen with a tap`() {
        val speech = FakeSpeechSynthesizer()
        show(speech)

        composeRule.onNodeWithContentDescription("Tom")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "100%"))
        composeRule.onNodeWithContentDescription("Velocidade").assertExists()

        composeRule.onNodeWithText("Criança").performScrollTo().performClick()

        assertEquals(VoicePreset.CHILD, preferences.preferences.value.voice.preset)
        assertEquals(listOf("Olá! Eu quero água, por favor."), speech.spoken)
    }

    @Test
    fun `without a Portuguese voice it says how to get one, instead of offering settings that do nothing`() {
        show(FakeSpeechSynthesizer(initial = SpeechState.UNAVAILABLE))

        composeRule.onNodeWithText("Instalar mais vozes").assertExists()
        composeRule.onNodeWithContentDescription("Tom").assertDoesNotExist()
    }
}
