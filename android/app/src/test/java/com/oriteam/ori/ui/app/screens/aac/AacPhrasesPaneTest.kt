package com.oriteam.ori.ui.app.screens.aac

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.R
import com.oriteam.ori.domain.aac.AacMessage
import com.oriteam.ori.domain.aac.MessagePart
import com.oriteam.ori.domain.aac.SavedMessage
import com.oriteam.ori.domain.aac.SpeechState
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.ui.app.screens.common.messageRes
import com.oriteam.ori.ui.components.state.ContentState
import com.oriteam.ori.ui.theme.OriTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AacPhrasesPaneTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val said = mutableListOf<String>()

    private fun render(saved: ContentState<List<SavedMessage>>, speechState: SpeechState = SpeechState.READY) {
        composeRule.setContent {
            OriTheme {
                AacPhrasesPane(
                    saved = saved,
                    speechState = speechState,
                    onSay = { said += it },
                    onStop = {},
                    onDeleteSaved = {},
                    onRetry = {}
                )
            }
        }
    }

    @Test
    fun `with nothing saved the ready phrases already work with one tap`() {
        render(ContentState.Empty)

        composeRule.onNodeWithText("Preciso de ajuda").performClick()

        assertEquals(listOf("Preciso de ajuda"), said)
    }

    @Test
    fun `a failure to read saved phrases is shown and does not block the ready ones`() {
        val error = DataError.PERMISSION_DENIED.messageRes()
        render(ContentState.Error(error))

        composeRule.onNodeWithText(composeRule.activity.getString(error)).assertExists()
        composeRule.onNodeWithText("Estou com dor").performClick()
        assertEquals(listOf("Estou com dor"), said)
    }

    @Test
    fun `the person's own phrases come first`() {
        render(ContentState.Content(listOf(SavedMessage("m1", "Quero ligar para casa"))))

        composeRule.onNodeWithText("Quero ligar para casa").performClick()
        assertEquals(listOf("Quero ligar para casa"), said)
    }

    @Test
    fun `while speaking it can be stopped`() {
        render(ContentState.Empty, SpeechState.SPEAKING)

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.aac_stop)).assertExists()
    }

    @Test
    fun `ready phrases are unique, since each is a list key, and all have a picture`() {
        val texts = readyPhrases.map { composeRule.activity.getString(it.textRes) }

        assertEquals(texts.size, texts.toSet().size)
        assertTrue(readyPhrases.all { it.emoji.isNotBlank() })
    }

    @Test
    fun `old symbols with phrases no longer repeat words in a built message`() {
        val message = AacMessage(
            listOf(
                MessagePart(label = "estou", spokenText = "estou"),
                MessagePart(label = "Sono", spokenText = "Estou com sono"),
                MessagePart(label = "Dor", spokenText = "Estou com dor")
            )
        )

        assertEquals("Estou com Sono Dor", message.text)
    }
}
