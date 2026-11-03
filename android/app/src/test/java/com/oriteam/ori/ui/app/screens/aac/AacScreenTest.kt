package com.oriteam.ori.ui.app.screens.aac

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.domain.aac.SpeechState
import com.oriteam.ori.domain.preferences.AacPreferences
import com.oriteam.ori.domain.preferences.SymbolLabelMode
import com.oriteam.ori.domain.preferences.SymbolSize
import com.oriteam.ori.testing.OriTestWindow
import com.oriteam.ori.testing.bounds
import com.oriteam.ori.ui.components.state.ContentState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AacScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val symbols = listOf("Água", "Comer", "Banheiro", "Ajuda", "Dor", "Sono").map {
        AacSymbolUiModel(id = it, label = it, image = SymbolImageUi.Emoji("💧"))
    }
    private val removed = mutableListOf<Int>()
    private val tapped = mutableListOf<String>()

    private fun boardState(
        preferences: AacPreferences = AacPreferences(),
        speechState: SpeechState = SpeechState.READY,
        message: List<AacSymbolUiModel> = emptyList()
    ) = AacCommunicateUiState(
        boards = ContentState.Content(listOf(BoardTabUiModel("b1", "Básico"))),
        selection = AacSelection.Board("b1"),
        symbols = ContentState.Content(symbols),
        message = message,
        preferences = preferences,
        speechState = speechState
    )

    private val actions = AacCommunicateActions(
        message = AacMessageBarActions.None.copy(onRemovePart = { removed += it }),
        onSelect = {},
        onShowBoards = {},
        onSymbolTap = { tapped += it },
        onSayPhrase = {},
        onDeleteSavedRequest = {},
        onCreateStarterBoard = {},
        onRetry = {},
        onDismissDisplay = {},
        onDeleteSavedConfirm = {},
        onDeleteSavedDismiss = {}
    )

    private fun render(state: AacCommunicateUiState, widthDp: Int = 400, heightDp: Int = 900, fontScale: Float = 1f) {
        composeRule.setContent {
            OriTestWindow(widthDp = widthDp, heightDp = heightDp, fontScale = fontScale) {
                AacCommunicateScreen(state = state, actions = actions)
            }
        }
    }

    private fun rowOf(label: String) = composeRule.onNodeWithText(label).bounds.top

    @Test
    fun `each symbol is one button that says its label and what a tap does`() {
        render(boardState())

        composeRule.onNodeWithText("Água")
            .assert(hasClickAction())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, androidx.compose.ui.semantics.Role.Button))
            .performClick()

        assertEquals(listOf("Água"), tapped)
    }

    @Test
    fun `a hidden label is still read by screen readers`() {
        render(boardState(AacPreferences(labelMode = SymbolLabelMode.IMAGE_ONLY)))

        composeRule.onNodeWithText("Banheiro").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Banheiro").assert(hasClickAction()).assertIsDisplayed()
    }

    @Test
    fun `a narrow screen stacks symbols in more rows than a wide one`() {
        render(boardState().copy(preferences = AacPreferences(symbolSize = SymbolSize.MEDIUM)), widthDp = 400)
        // 400 dp holds three medium symbols per row: the fourth starts a new row.
        assertEquals(rowOf("Água"), rowOf("Banheiro"))
        assertNotEquals(rowOf("Água"), rowOf("Ajuda"))
    }

    @Test
    fun `a wide screen shows more symbols per row`() {
        render(boardState(), widthDp = 1000)

        assertEquals(rowOf("Água"), rowOf("Ajuda"))
    }

    @Test
    fun `enlarged text gives fewer symbols per row instead of cut labels`() {
        render(boardState(), widthDp = 400, heightDp = 2400, fontScale = 2f)

        // Three per row at the normal text size; the third symbol now starts a new row.
        assertEquals(rowOf("Água"), rowOf("Comer"))
        assertNotEquals(rowOf("Água"), rowOf("Banheiro"))
        composeRule.onNodeWithText("Banheiro").assertIsDisplayed()
    }

    @Test
    fun `without a voice the person is told the message will be shown instead`() {
        render(boardState(speechState = SpeechState.UNAVAILABLE))

        composeRule.onNodeWithText("Sem voz neste aparelho", substring = true).assertIsDisplayed()
    }

    @Test
    fun `the message announces its content and each piece can be removed`() {
        render(boardState(message = symbols.take(2)))

        composeRule.onNodeWithContentDescription("Mensagem: Água Comer")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
        composeRule.onNodeWithContentDescription("Remover Comer").performClick()

        assertEquals(listOf(1), removed)
    }

    @Test
    fun `without boards the person can still type, or choose how to start`() {
        render(AacCommunicateUiState(boards = ContentState.Empty, selection = null, symbols = ContentState.Empty))

        composeRule.onNodeWithContentDescription("Escrever").assertIsDisplayed()
        composeRule.onNodeWithText("Usar a prancha sugerida").assertIsDisplayed()
        composeRule.onNodeWithText("Montar minha prancha").assertIsDisplayed()
    }

    @Test
    fun `an organizer without permission offers no changes`() {
        val rows = listOf(OrganizerItemUiModel(BoardTabUiModel("b1", "Básico"), false, true))
        composeRule.setContent {
            OriTestWindow(widthDp = 400, heightDp = 800) {
                BoardsPane(
                    state = AacOrganizeUiState(canManage = false, boards = ContentState.Content(rows)),
                    title = "Pat",
                    onNavigateBack = null,
                    onOpenBoard = {},
                    onMoveBoard = { _, _ -> },
                    onCreateBoard = {},
                    onRetry = {}
                )
            }
        }

        composeRule.onNodeWithText("Básico").assertIsDisplayed()
        composeRule.onNodeWithText("Nova prancha").assertDoesNotExist()
        assertEquals(
            0,
            composeRule.onAllNodesWithContentDescription("Mover Básico para depois").fetchSemanticsNodes().size
        )
    }

    @Test
    fun `move buttons name the item they move`() {
        val rows = listOf(OrganizerItemUiModel(BoardTabUiModel("b1", "Básico"), false, true))
        composeRule.setContent {
            OriTestWindow(widthDp = 400, heightDp = 800) {
                BoardsPane(
                    state = AacOrganizeUiState(canManage = true, boards = ContentState.Content(rows)),
                    title = "Organizar",
                    onNavigateBack = null,
                    onOpenBoard = {},
                    onMoveBoard = { _, _ -> },
                    onCreateBoard = {},
                    onRetry = {}
                )
            }
        }

        composeRule.onNode(hasContentDescription("Mover Básico para depois")).assertIsDisplayed()
        composeRule.onNodeWithText("Nova prancha").assertIsDisplayed()
    }
}
