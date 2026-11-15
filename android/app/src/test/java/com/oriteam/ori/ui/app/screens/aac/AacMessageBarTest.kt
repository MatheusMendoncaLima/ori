package com.oriteam.ori.ui.app.screens.aac

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.domain.aac.SpeechState
import com.oriteam.ori.testing.OriTestWindow
import com.oriteam.ori.testing.bounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AacMessageBarTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val pieces = listOf("Eu", "Quero", "Água").map { AacSymbolUiModel(id = it, label = it) }
    private val calls = mutableListOf<String>()
    private val actions = AacMessageBarActions(
        onRemovePart = { calls += "remove:$it" },
        onRemoveLast = { calls += "removeLast" },
        onClear = { calls += "clear" },
        onSpeak = { calls += "speak" },
        onStop = { calls += "stop" },
        onShow = { calls += "show" },
        onSave = { calls += "save" },
        onTyped = { calls += "typed:$it" }
    )

    private fun hasStateDescription(value: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

    private fun render(
        message: List<AacSymbolUiModel> = pieces,
        speechState: SpeechState = SpeechState.READY,
        canSave: Boolean = true,
        isFull: Boolean = false
    ) {
        composeRule.setContent {
            OriTestWindow(widthDp = 360, heightDp = 616) {
                Column {
                    AacMessageBar(message = message, speechState = speechState, isFull = isFull, actions = actions)
                    AacMessageActions(
                        hasMessage = message.isNotEmpty(),
                        speechState = speechState,
                        canSave = canSave,
                        actions = actions
                    )
                }
            }
        }
    }

    private fun openMenu() = composeRule.onNodeWithContentDescription("Mais ações da mensagem").performClick()

    private val textField get() = composeRule.onNode(hasSetTextAction())

    /** The keyboard with "Escrever" under it, at the end of the message. */
    private val writeAction get() = composeRule.onNodeWithContentDescription("Escrever")

    @Test
    fun `writing is a button with a full touch target`() {
        render()

        val write = writeAction.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)).bounds

        assertTrue("width ${write.width}", write.width >= 48.dp)
        assertTrue("height ${write.height}", write.height >= 48.dp)
    }

    @Test
    fun `the four actions fit in one row on a common phone`() {
        render()

        val tops = listOf(
            composeRule.onNodeWithContentDescription("Apagar última"),
            composeRule.onNodeWithContentDescription("Salvar frase"),
            composeRule.onNodeWithText("Falar"),
            composeRule.onNodeWithContentDescription("Mais ações da mensagem")
        ).map { it.assertIsDisplayed().assert(hasClickAction()).bounds.top }

        assertEquals(1, tops.distinct().size)
    }

    @Test
    fun `enlarged text moves actions to a new row and never breaks a word`() {
        var textScale by mutableFloatStateOf(1f)
        composeRule.setContent {
            OriTestWindow(widthDp = 360, heightDp = 616, fontScale = textScale) {
                AacMessageActions(
                    hasMessage = true,
                    speechState = SpeechState.READY,
                    canSave = true,
                    actions = actions
                )
            }
        }

        listOf(1f, 1.15f, 1.3f, 1.5f, 1.8f, 2f).forEach { scale ->
            textScale = scale
            composeRule.waitForIdle()
            assertEquals("at text scale $scale", 1, lineCount("Salvar"))
        }
        // Past what a row can hold: two actions per row, aligned.
        textScale = 3.5f
        composeRule.waitForIdle()
        val erase = composeRule.onNodeWithContentDescription("Apagar última").bounds
        val clear = composeRule.onNodeWithContentDescription("Salvar frase").bounds
        val speak = composeRule.onNodeWithText("Falar").bounds
        assertEquals(erase.top, clear.top)
        assertEquals(erase.left, speak.left)
        assertTrue(speak.top >= erase.bottom)
    }

    private fun lineCount(text: String): Int {
        val node = composeRule.onNode(hasText(text), useUnmergedTree = true).fetchSemanticsNode()
        val results = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.single().lineCount
    }

    @Test
    fun `speaking turns the main action into stop`() {
        render(speechState = SpeechState.SPEAKING)

        composeRule.onNodeWithText("Falar").assertDoesNotExist()
        composeRule.onNodeWithText("Parar").performClick()

        assertEquals(listOf("stop"), calls)
    }

    @Test
    fun `speak and erase the last piece act on the message`() {
        render()

        composeRule.onNodeWithText("Falar").performClick()
        composeRule.onNodeWithContentDescription("Apagar última").performClick()
        composeRule.onNodeWithContentDescription("Remover Quero").performClick()

        assertEquals(listOf("speak", "removeLast", "remove:1"), calls)
    }

    @Test
    fun `saving is one tap away and the menu holds the rarer actions`() {
        render()

        composeRule.onNodeWithContentDescription("Salvar frase").performClick()
        openMenu()
        composeRule.onNodeWithText("Mostrar em letras grandes").performClick()
        openMenu()
        composeRule.onNodeWithText("Apagar tudo").performClick()

        assertEquals(listOf("save", "show", "clear"), calls)
    }

    @Test
    fun `without a message nothing that needs one can be used`() {
        render(message = emptyList(), canSave = false)

        composeRule.onNodeWithText("Falar").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Apagar última").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Salvar frase").assertIsNotEnabled()
        writeAction.assertIsEnabled()
        openMenu()
        composeRule.onNodeWithText("Mostrar em letras grandes").assertIsNotEnabled()
        composeRule.onNodeWithText("Apagar tudo").assertIsNotEnabled()
    }

    @Test
    fun `a message too long to save keeps the other actions`() {
        render(canSave = false)

        composeRule.onNodeWithContentDescription("Salvar frase").assertIsNotEnabled()
        openMenu()
        composeRule.onNodeWithText("Mostrar em letras grandes").assertIsEnabled()
    }

    @Test
    fun `the text field opens on request, focused, and says its state`() {
        render()
        textField.assertDoesNotExist()
        writeAction.assert(hasStateDescription("campo de texto fechado"))

        writeAction.performClick()

        textField.assertIsDisplayed().assertIsFocused()
        writeAction.assert(hasStateDescription("campo de texto aberto"))
    }

    @Test
    fun `a typed word goes to the message and the field stays open for the next one`() {
        render()
        writeAction.performClick()

        composeRule.onNodeWithText("Adicionar").assertIsNotEnabled()
        textField.performTextInput("gelada")
        composeRule.onNodeWithText("Adicionar").performClick()

        assertEquals(listOf("typed:gelada"), calls)
        textField.assertIsDisplayed()
        composeRule.onNodeWithText("gelada").assertDoesNotExist()
    }

    @Test
    fun `closing the field keeps what was typed`() {
        render()
        writeAction.performClick()
        textField.performTextInput("gelada")

        writeAction.performClick()
        textField.assertDoesNotExist()
        writeAction.performClick()

        composeRule.onNodeWithText("gelada").assertIsDisplayed()
        assertTrue(calls.isEmpty())
    }

    @Test
    fun `a full message keeps the typed text instead of losing it`() {
        render(isFull = true)
        writeAction.performClick()
        textField.performTextInput("gelada")

        composeRule.onNodeWithText("Adicionar").assertIsNotEnabled()

        composeRule.onNodeWithText("gelada").assertIsDisplayed()
        composeRule.onNodeWithText("A mensagem chegou ao limite", substring = true).assertIsDisplayed()
    }
}
