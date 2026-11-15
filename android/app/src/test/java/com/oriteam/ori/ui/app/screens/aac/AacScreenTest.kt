package com.oriteam.ori.ui.app.screens.aac

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.height
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
    private val selected = mutableListOf<AacSelection>()
    private var volumeRaised = 0
    private var volumeWarningClosed = 0

    private fun boardState(
        preferences: AacPreferences = AacPreferences(),
        speechState: SpeechState = SpeechState.READY,
        message: List<AacSymbolUiModel> = emptyList()
    ) = AacCommunicateUiState(
        boards = ContentState.Content(listOf(BoardTabUiModel("b1", "Básico"))),
        selection = AacSelection.Board("b1"),
        buildBoard = AacSelection.Board("b1"),
        symbols = ContentState.Content(symbols),
        message = message,
        preferences = preferences,
        speechState = speechState
    )

    private val actions = AacCommunicateActions(
        message = AacMessageBarActions.None.copy(onRemovePart = { removed += it }),
        onSelect = { selected += it },
        onShowBoards = {},
        onSymbolTap = { tapped += it },
        onSayPhrase = { _, _ -> },
        onDeleteSavedRequest = {},
        onCreateStarterBoard = {},
        onRetry = {},
        onDismissDisplay = {},
        onDeleteSavedConfirm = {},
        onDeleteSavedDismiss = {},
        onRaiseVolume = { volumeRaised++ },
        onDismissVolumeWarning = { volumeWarningClosed++ }
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
    fun `Montar is made ahead while Prontas is open, out of reach, and shown at once`() {
        var state by mutableStateOf(
            boardState().copy(selection = AacSelection.SavedMessages, savedMessages = ContentState.Content(emptyList()))
        )
        composeRule.setContent {
            OriTestWindow(widthDp = 400, heightDp = 900) { AacCommunicateScreen(state = state, actions = actions) }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Estou com dor").assertIsDisplayed()
        composeRule.onNodeWithText("Água").assertDoesNotExist()

        state = boardState()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Água").assertIsDisplayed().performClick()
        assertEquals(listOf("Água"), tapped)
        composeRule.onNodeWithText("Estou com dor").assertDoesNotExist()
    }

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
    fun `a voice nobody heard shows the warning, with buttons to turn the volume up or close it`() {
        render(boardState().copy(isVolumeOff = true), widthDp = 360)

        composeRule.onNodeWithText("O volume está no zero: ninguém ouviu a voz.").assertIsDisplayed()
        composeRule.onNodeWithText("Aumentar o volume").assert(hasClickAction()).performClick()
        composeRule.onNodeWithText("Fechar").assert(hasClickAction()).performClick()

        assertEquals(1, volumeRaised)
        assertEquals(1, volumeWarningClosed)
    }

    @Test
    fun `without the warning of volume there is nothing about it on the screen`() {
        render(boardState())

        composeRule.onNodeWithText("Aumentar o volume").assertDoesNotExist()
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
    fun `Mudar prancha in the Mais menu lists the boards and opens the one chosen`() {
        val boards = listOf(BoardTabUiModel("b1", "Básico"), BoardTabUiModel("b2", "Comida"))
        render(boardState().copy(boards = ContentState.Content(boards)))

        composeRule.onNodeWithContentDescription("Mais ações da mensagem").performClick()
        composeRule.onNodeWithText("Mudar prancha").performClick()
        composeRule.onNodeWithText("Escolha uma prancha").assertIsDisplayed()
        composeRule.onNode(hasText("Básico") and isRadio).assertIsSelected()
        composeRule.onNode(hasText("Comida") and isRadio).performClick()

        assertEquals(listOf<AacSelection>(AacSelection.Board("b2")), selected)
        composeRule.onNodeWithText("Escolha uma prancha").assertDoesNotExist()
    }

    @Test
    fun `without boards there is no board to change to`() {
        render(boardState().copy(boards = ContentState.Empty, selection = null, buildBoard = null))

        composeRule.onNodeWithContentDescription("Mais ações da mensagem").performClick()
        composeRule.onNodeWithText("Mudar prancha").assertDoesNotExist()
    }

    @Test
    fun `a word without a picture is as tall as one with a picture, and so is Nova palavra`() {
        val words = listOf(
            AacSymbolUiModel(id = "1", label = "água", image = SymbolImageUi.Emoji("🥛")),
            AacSymbolUiModel(id = "2", label = "com", image = null)
        )
        composeRule.setContent {
            OriTestWindow(widthDp = 360, heightDp = 800) {
                AacBoardGrid(
                    symbols = words,
                    preferences = AacPreferences(),
                    onSymbolClick = {},
                    onClickLabel = "",
                    onAddSymbol = {}
                )
            }
        }

        // Each tile is one button, so its text is found on the tile itself.
        val withPicture = composeRule.onNodeWithText("água").bounds.height
        assertEquals(withPicture, composeRule.onNodeWithText("com").bounds.height)
        assertEquals(withPicture, composeRule.onNodeWithText("Nova palavra").bounds.height)
    }

    @Test
    fun `screen readers move a symbol with actions, only where it can go, and only when moving is on`() {
        val words = listOf(
            AacSymbolUiModel(id = "1", label = "água", image = SymbolImageUi.Emoji("🥛")),
            AacSymbolUiModel(id = "2", label = "com", image = null)
        )
        val moves = mutableListOf<Pair<String, Int>>()
        var canMove by mutableStateOf(true)
        composeRule.setContent {
            OriTestWindow(widthDp = 360, heightDp = 800) {
                AacBoardGrid(
                    symbols = words,
                    preferences = AacPreferences(),
                    onSymbolClick = {},
                    onClickLabel = "",
                    onMoveSymbol = { id: String, to: Int -> moves += id to to }.takeIf { canMove }
                )
            }
        }
        fun actionsOf(label: String) =
            composeRule.onNodeWithText(label).fetchSemanticsNode().config.getOrNull(SemanticsActions.CustomActions)

        assertEquals(listOf("Mover água para depois"), actionsOf("água")?.map { it.label })
        assertEquals(listOf("Mover com para antes"), actionsOf("com")?.map { it.label })
        actionsOf("com")!!.single().action()
        assertEquals(listOf("2" to 0), moves)

        canMove = false
        composeRule.waitForIdle()
        assertEquals(null, actionsOf("água")?.takeIf { it.isNotEmpty() })
    }

    private val isRadio = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)

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
    fun `an older suggested board offers the words it lacks, and only then`() {
        var added = 0
        var state by mutableStateOf(
            AacOrganizeUiState(canManage = true, symbols = ContentState.Empty, suggestedWordsToAdd = 16)
        )
        composeRule.setContent {
            OriTestWindow(widthDp = 400, heightDp = 800) {
                SymbolsPane(
                    state = state,
                    onNavigateBack = null,
                    onEditSymbol = {},
                    onAddSymbol = {},
                    onMoveSymbol = { _, _ -> },
                    onRenameBoard = {},
                    onDeleteBoard = {},
                    onRetry = {},
                    onAddSuggestedWords = { added++ }
                )
            }
        }

        composeRule.onNodeWithText("Acrescentar as 16 palavras sugeridas que faltam").performClick()
        assertEquals(1, added)
        state = state.copy(suggestedWordsToAdd = 0)
        composeRule.onNodeWithText("Acrescentar as 16 palavras sugeridas que faltam").assertDoesNotExist()
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

    private fun renderSymbolsPane(canManage: Boolean, onRename: () -> Unit = {}, onDelete: () -> Unit = {}) {
        composeRule.setContent {
            OriTestWindow(widthDp = 360, heightDp = 640) {
                SymbolsPane(
                    state = AacOrganizeUiState(canManage = canManage, symbols = ContentState.Empty),
                    onNavigateBack = null,
                    onEditSymbol = {},
                    onAddSymbol = {},
                    onMoveSymbol = { _, _ -> },
                    onRenameBoard = onRename,
                    onDeleteBoard = onDelete,
                    onRetry = {}
                )
            }
        }
    }

    @Test
    fun `renaming and deleting a board wait in the menu of the title bar`() {
        var renamed = 0
        var deleted = 0
        renderSymbolsPane(canManage = true, onRename = { renamed++ }, onDelete = { deleted++ })

        composeRule.onNodeWithText("Renomear prancha").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Mais opções da prancha").performClick()
        composeRule.onNodeWithText("Renomear prancha").performClick()
        composeRule.onNodeWithContentDescription("Mais opções da prancha").performClick()
        composeRule.onNodeWithText("Excluir prancha").performClick()

        assertEquals(1, renamed)
        assertEquals(1, deleted)
    }

    @Test
    fun `who cannot manage a board has no menu to rename or delete it`() {
        renderSymbolsPane(canManage = false)

        composeRule.onNodeWithContentDescription("Mais opções da prancha").assertDoesNotExist()
    }
}
