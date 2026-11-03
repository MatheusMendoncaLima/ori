package com.oriteam.ori.ui.app.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.domain.aac.SpeechState
import com.oriteam.ori.domain.preferences.AacPreferences
import com.oriteam.ori.testing.OriTestWindow
import com.oriteam.ori.testing.bounds
import com.oriteam.ori.ui.app.screens.aac.AacBoardGrid
import com.oriteam.ori.ui.app.screens.aac.AacBoardLayout
import com.oriteam.ori.ui.app.screens.aac.AacMessageActions
import com.oriteam.ori.ui.app.screens.aac.AacMessageBar
import com.oriteam.ori.ui.app.screens.aac.AacMessageBarActions
import com.oriteam.ori.ui.app.screens.aac.AacSymbolUiModel
import com.oriteam.ori.ui.app.screens.routine.RoutineStepUiModel
import com.oriteam.ori.ui.app.screens.routine.RoutineTimeline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeatureComponentsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val symbols = (1..12).map { AacSymbolUiModel(id = "s$it", label = "Símbolo $it") }

    private fun countSymbolsInFirstRow(): Int {
        val firstRowTop = composeRule.onNodeWithText("Símbolo 1").bounds.top
        return symbols.count { symbol ->
            val nodes = composeRule.onAllNodesWithText(symbol.label).fetchSemanticsNodes()
            nodes.isNotEmpty() && composeRule.onNodeWithText(symbol.label).bounds.top == firstRowTop
        }
    }

    @Test
    fun `communication board adds symbols per row when the window grows`() {
        var windowWidth by mutableIntStateOf(360)
        composeRule.setContent {
            OriTestWindow(widthDp = windowWidth, heightDp = 800) {
                AacBoardGrid(symbols, AacPreferences(), onSymbolClick = {}, onClickLabel = "adicionar")
            }
        }
        val narrowColumns = countSymbolsInFirstRow()

        windowWidth = 1280
        composeRule.waitForIdle()
        val wideColumns = countSymbolsInFirstRow()

        assertTrue("narrow=$narrowColumns wide=$wideColumns", wideColumns > narrowColumns)
        assertTrue(narrowColumns >= 2)
    }

    @Test
    fun `message bar removes a tapped symbol and enables speaking only with a message`() {
        val removed = mutableListOf<Int>()
        var spoken = 0
        composeRule.setContent {
            OriTestWindow(widthDp = 411, heightDp = 891) {
                val actions = AacMessageBarActions.None.copy(onRemovePart = { removed += it }, onSpeak = { spoken++ })
                Column {
                    AacMessageBar(
                        message = symbols.take(2),
                        speechState = SpeechState.READY,
                        isFull = false,
                        actions = actions
                    )
                    AacMessageActions(
                        hasMessage = true,
                        speechState = SpeechState.READY,
                        canSave = true,
                        actions = actions
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription("Remover Símbolo 2").performClick()
        composeRule.onNodeWithText("Falar").assertIsEnabled().performClick()

        assertEquals(listOf(1), removed)
        assertEquals(1, spoken)
    }

    @Test
    fun `empty message shows a hint and disables speaking`() {
        composeRule.setContent { OriTestWindow(widthDp = 411, heightDp = 891) { board() } }

        composeRule.onNodeWithText("Toque nos símbolos para montar uma frase").assertExists()
        composeRule.onNodeWithText("Falar").assertIsNotEnabled()
    }

    @Test
    fun `landscape phone places the sentence beside the symbols`() {
        composeRule.setContent { OriTestWindow(widthDp = 891, heightDp = 411) { board() } }

        val hint = composeRule.onNodeWithText("Toque nos símbolos para montar uma frase").bounds
        val firstSymbol = composeRule.onNodeWithText("Símbolo 1").bounds
        assertTrue(hint.right <= firstSymbol.left)
    }

    @Composable
    private fun board() {
        AacBoardLayout(
            messageBar = {
                AacMessageBar(
                    emptyList(),
                    SpeechState.READY,
                    isFull = false,
                    AacMessageBarActions.None,
                    it
                )
            },
            content = { AacBoardGrid(symbols, AacPreferences(), onSymbolClick = {}, onClickLabel = "adicionar", it) },
            actions = {
                AacMessageActions(
                    hasMessage = false,
                    speechState = SpeechState.READY,
                    canSave = false,
                    actions = AacMessageBarActions.None
                )
            }
        )
    }

    @Test
    fun `routine step toggles by its id, announces its state and opens details separately`() {
        val toggled = mutableListOf<String>()
        val opened = mutableListOf<String>()
        composeRule.setContent {
            OriTestWindow(widthDp = 411, heightDp = 891) {
                RoutineTimeline(
                    steps = listOf(RoutineStepUiModel(id = "breakfast", title = "Tomar café", isDone = true)),
                    onToggleDone = { toggled += it },
                    onOpen = { opened += it }
                )
            }
        }

        composeRule.onNodeWithContentDescription("Concluir: Tomar café")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Concluída"))
            .performClick()
        composeRule.onNodeWithText("Tomar café").performClick()

        assertEquals(listOf("breakfast"), toggled)
        assertEquals(listOf("breakfast"), opened)
    }

    @Test
    fun `an activity the viewer may not complete has a disabled toggle`() {
        composeRule.setContent {
            OriTestWindow(widthDp = 411, heightDp = 891) {
                RoutineTimeline(
                    steps = listOf(RoutineStepUiModel(id = "x", title = "Remédio", canToggle = false)),
                    onToggleDone = {},
                    onOpen = null
                )
            }
        }

        composeRule.onNodeWithContentDescription("Concluir: Remédio").assertIsNotEnabled()
    }
}
