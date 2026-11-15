package com.oriteam.ori.ui.app.screens.aac

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.testing.OriTestWindow
import com.oriteam.ori.testing.bounds
import com.oriteam.ori.ui.components.state.ContentState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

/** A common phone (360 x 780 dp) without the status bar and the bottom navigation, drawn elsewhere. */
private const val PHONE_WIDTH_DP = 360
private const val PHONE_HEIGHT_DP = 616

/** How much of the screen the symbols get, and whether they stay put while the message changes. */
@RunWith(AndroidJUnit4::class)
class AacBoardLayoutTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val symbols = (1..12).map {
        AacSymbolUiModel(id = "s$it", label = "Símbolo $it", image = SymbolImageUi.Emoji("💧"))
    }
    private val pieces = (1..12).map { AacSymbolUiModel(id = "p$it", label = "Pedaço $it") }

    private val actions = AacCommunicateActions(
        message = AacMessageBarActions.None,
        onSelect = {},
        onShowBoards = {},
        onSymbolTap = {},
        onSayPhrase = { _, _ -> },
        onDeleteSavedRequest = {},
        onCreateStarterBoard = {},
        onRetry = {},
        onDismissDisplay = {},
        onDeleteSavedConfirm = {},
        onDeleteSavedDismiss = {}
    )

    private fun boardState(message: List<AacSymbolUiModel> = emptyList()) = AacCommunicateUiState(
        boards = ContentState.Content(listOf(BoardTabUiModel("b1", "Básico"))),
        selection = AacSelection.Board("b1"),
        buildBoard = AacSelection.Board("b1"),
        symbols = ContentState.Content(symbols),
        message = message
    )

    private fun render(
        state: AacCommunicateUiState,
        widthDp: Int = PHONE_WIDTH_DP,
        heightDp: Int = PHONE_HEIGHT_DP,
        fontScale: Float = 1f
    ) {
        composeRule.setContent {
            OriTestWindow(widthDp = widthDp, heightDp = heightDp, fontScale = fontScale) {
                AacCommunicateScreen(state = state, actions = actions)
            }
        }
    }

    private fun symbolTop(label: String) = composeRule.onNodeWithText(label).bounds.top

    private fun piece(label: String) = composeRule.onNodeWithContentDescription("Remover $label")

    @Test
    fun `a common phone gives most of the screen to the symbols`() {
        render(boardState())

        assertTrue("first row at ${symbolTop("Símbolo 1")}", symbolTop("Símbolo 1") < (PHONE_HEIGHT_DP * 0.45f).dp)
        // Three columns: the ninth symbol is in the third row, entirely on screen.
        val thirdRow = composeRule.onNodeWithText("Símbolo 9").assertIsDisplayed().bounds
        assertTrue("third row ends at ${thirdRow.bottom}", thirdRow.bottom <= PHONE_HEIGHT_DP.dp)
    }

    @Test
    fun `the symbols stay in place while the message grows`() {
        var message by mutableStateOf(pieces.take(1))
        composeRule.setContent {
            OriTestWindow(widthDp = PHONE_WIDTH_DP, heightDp = PHONE_HEIGHT_DP) {
                AacCommunicateScreen(state = boardState(message), actions = actions)
            }
        }
        val before = symbolTop("Símbolo 1")

        message = pieces
        composeRule.waitForIdle()

        assertEquals(before, symbolTop("Símbolo 1"))
    }

    @Test
    fun `a long message shows its newest piece`() {
        render(boardState(message = pieces))

        piece("Pedaço 12").assertIsDisplayed()
        piece("Pedaço 1").assertIsNotDisplayed()
    }

    @Test
    fun `enlarged text and the open text field never push the symbols off the screen`() {
        render(boardState(message = pieces), fontScale = 2f)

        composeRule.onNodeWithContentDescription("Escrever").performClick()

        composeRule.onNodeWithText("Falar").assertIsDisplayed()
        composeRule.onNodeWithText("Símbolo 1").assertIsDisplayed()
        assertTrue(symbolTop("Símbolo 1") < (PHONE_HEIGHT_DP * 0.75f).dp)
    }

    @Test
    fun `without boards the ways to start fit on a common phone`() {
        render(AacCommunicateUiState(boards = ContentState.Empty, selection = null, symbols = ContentState.Empty))

        composeRule.onNodeWithText("Usar a prancha sugerida").assertIsDisplayed()
        val lastOption = composeRule.onNodeWithText("Montar minha prancha").assertIsDisplayed().bounds
        assertTrue("last option ends at ${lastOption.bottom}", lastOption.bottom <= PHONE_HEIGHT_DP.dp)
    }

    @Test
    fun `a label without a picture stays centered in its tile`() {
        val textOnly = symbols.map { it.copy(image = null) }
        render(boardState().copy(symbols = ContentState.Content(textOnly)))

        val label = composeRule.onNode(hasText("Símbolo 1"), useUnmergedTree = true).bounds
        val tile = composeRule.onNodeWithText("Símbolo 1").bounds
        val spaceAbove = label.top - tile.top
        val spaceBelow = tile.bottom - label.bottom

        assertTrue("above $spaceAbove, below $spaceBelow", abs(spaceAbove.value - spaceBelow.value) <= 1f)
    }

    @Test
    fun `a landscape phone keeps the symbols small enough to show several rows`() {
        render(boardState(), widthDp = 891, heightDp = 411)

        // Five columns beside the message: the fifth symbol is still in the first row.
        assertEquals(symbolTop("Símbolo 1"), symbolTop("Símbolo 5"))
        composeRule.onNodeWithText("Símbolo 6").assertIsDisplayed()
    }

    @Test
    fun `beside the symbols a long message wraps and stays fully visible`() {
        render(boardState(message = pieces.take(3)), widthDp = 891, heightDp = 411)

        assertTrue(piece("Pedaço 3").assertIsDisplayed().bounds.top > piece("Pedaço 1").assertIsDisplayed().bounds.top)
    }
}
