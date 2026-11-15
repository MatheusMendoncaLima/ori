package com.oriteam.ori.ui.app.screens.aac

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.R
import com.oriteam.ori.domain.aac.SavedMessage
import com.oriteam.ori.domain.aac.SpeechState
import com.oriteam.ori.domain.aac.moved
import com.oriteam.ori.domain.preferences.AacPreferences
import com.oriteam.ori.testing.OriTestWindow
import com.oriteam.ori.testing.onScreenRoot
import com.oriteam.ori.ui.components.list.STORE_WAIT_MILLIS
import com.oriteam.ori.ui.components.state.ContentState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.time.Duration

private const val HOLD_MILLIS = 1_000L
private const val DRAG_STEPS = 10

/** Longer than [STORE_WAIT_MILLIS]: an order never stored has given way by then. */
private const val NEVER_STORED_MILLIS = STORE_WAIT_MILLIS + 1_000L

/**
 * Holding and dragging a symbol in Montar, or a phrase (the person's or one that comes with the app), as on ORI Home:
 * let go, the item stays where it was dropped while the new order is stored, instead of going back to its old place
 * for that moment and then travelling to the new one.
 */
@RunWith(AndroidJUnit4::class)
class ReorderDropTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val moves = mutableListOf<Pair<String, Int>>()

    private val words = listOf("Água", "Comer", "Banheiro", "Ajuda").map {
        AacSymbolUiModel(id = it, label = it, image = SymbolImageUi.Emoji("💧"))
    }
    private var symbols by mutableStateOf(words)

    private val phrases = listOf(SavedMessage("m1", "Oi"), SavedMessage("m2", "Tchau"), SavedMessage("m3", "Obrigado"))
    private var saved by mutableStateOf(phrases)
    private var ready by mutableStateOf(readyPhrases)

    private fun showBoard() {
        composeRule.setContent {
            OriTestWindow(widthDp = 360, heightDp = 800) {
                AacBoardGrid(
                    symbols = symbols,
                    preferences = AacPreferences(),
                    onSymbolClick = {},
                    onClickLabel = "",
                    onMoveSymbol = { id, to -> moves += id to to }
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun showPhrases(canMove: Boolean = true) {
        composeRule.setContent {
            OriTestWindow(widthDp = 360, heightDp = 800) {
                AacPhrasesPane(
                    saved = ContentState.Content(saved),
                    speechState = SpeechState.READY,
                    onSay = { _, _ -> },
                    onStop = {},
                    onDeleteSaved = {},
                    onRetry = {},
                    ready = ready,
                    onMoveSaved = { id: String, to: Int -> moves += id to to }.takeIf { canMove },
                    onMoveReady = { id: String, to: Int -> moves += id to to }.takeIf { canMove }
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun boundsOf(text: String): Rect = composeRule.onNodeWithText(text).fetchSemanticsNode().boundsInRoot

    private fun text(@StringRes res: Int): String = composeRule.activity.getString(res)

    private fun actionsOf(text: String) =
        composeRule.onNodeWithText(text).fetchSemanticsNode().config.getOrNull(SemanticsActions.CustomActions)
            .orEmpty().map { it.label }

    private fun holdAndDrag(from: String, to: String) {
        val start = boundsOf(from).center
        val step = (boundsOf(to).center - start) / DRAG_STEPS.toFloat()
        composeRule.onScreenRoot().performTouchInput { down(start) }
        composeRule.mainClock.advanceTimeBy(HOLD_MILLIS)
        repeat(DRAG_STEPS) {
            composeRule.onScreenRoot().performTouchInput { moveBy(step) }
            composeRule.waitForIdle()
        }
        composeRule.onScreenRoot().performTouchInput { up() }
        composeRule.waitForIdle()
    }

    private fun passTime(millis: Long) {
        composeRule.mainClock.advanceTimeBy(millis)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))
        composeRule.waitForIdle()
    }

    @Test
    fun `a symbol let go stays where it was dropped while the new order is stored`() {
        showBoard()
        val first = boundsOf("Água")
        val third = boundsOf("Banheiro")

        holdAndDrag("Banheiro", "Água")

        assertEquals(listOf("Banheiro" to 0), moves)
        // Not stored yet: it is where it was let go, not back in its old place.
        assertEquals(first.topLeft, boundsOf("Banheiro").topLeft)
        symbols = words.moved(2, 0)
        composeRule.waitForIdle()
        assertEquals(first.topLeft, boundsOf("Banheiro").topLeft)
        assertEquals(third.topLeft, boundsOf("Comer").topLeft)
    }

    @Test
    fun `an order that is never stored gives way to the stored one`() {
        showBoard()
        val third = boundsOf("Banheiro")

        holdAndDrag("Banheiro", "Água")
        passTime(NEVER_STORED_MILLIS)

        assertEquals(third.topLeft, boundsOf("Banheiro").topLeft)
    }

    @Test
    fun `the person's own phrases are moved by holding and dragging, and stay where they were dropped`() {
        showPhrases()
        val first = boundsOf("Oi")

        holdAndDrag("Obrigado", "Oi")

        assertEquals(listOf("m3" to 0), moves)
        assertEquals(first.topLeft, boundsOf("Obrigado").topLeft)
        assertTrue(boundsOf("Oi").top > first.top)
        saved = phrases.moved(2, 0)
        composeRule.waitForIdle()
        assertEquals(first.topLeft, boundsOf("Obrigado").topLeft)
    }

    @Test
    fun `screen readers move a phrase with actions, and only when moving is on`() {
        showPhrases()

        assertEquals(listOf("Mover Oi para depois"), actionsOf("Oi"))
        assertEquals(listOf("Mover Tchau para antes", "Mover Tchau para depois"), actionsOf("Tchau"))
        composeRule.onNodeWithText("Obrigado").fetchSemanticsNode().config[SemanticsActions.CustomActions]
            .first().action()
        assertEquals(listOf("m3" to 1), moves)
    }

    @Test
    fun `a phrase that comes with the app is moved among them, and stays where it was dropped`() {
        showPhrases()
        val yes = text(R.string.aac_phrase_yes)
        val help = text(R.string.aac_phrase_help)
        val first = boundsOf(yes)

        holdAndDrag(help, yes)

        assertEquals(listOf("help" to 0), moves)
        // Not stored yet: it is where it was let go, not back in its old place.
        assertEquals(first.topLeft, boundsOf(help).topLeft)
        ready = readyPhrasesIn(listOf("help"))
        composeRule.waitForIdle()
        assertEquals(first.topLeft, boundsOf(help).topLeft)
        assertTrue(boundsOf(yes).top >= first.top)
    }

    @Test
    fun `screen readers move a phrase that comes with the app, among those phrases`() {
        showPhrases()
        val yes = text(R.string.aac_phrase_yes)
        val no = text(R.string.aac_phrase_no)

        // The first ready phrase cannot go before: the saved ones are a list of their own.
        assertEquals(listOf("Mover $yes para depois"), actionsOf(yes))
        assertEquals(listOf("Mover $no para antes", "Mover $no para depois"), actionsOf(no))
        composeRule.onNodeWithText(no).fetchSemanticsNode().config[SemanticsActions.CustomActions]
            .first().action()
        assertEquals(listOf("no" to 0), moves)
    }

    @Test
    fun `with moving off a phrase held and dragged is not moved, and readers get no actions`() {
        showPhrases(canMove = false)
        assertTrue(actionsOf("Oi").isEmpty())
        assertTrue(actionsOf(text(R.string.aac_phrase_no)).isEmpty())

        // The finger scrolls the list instead.
        holdAndDrag("Obrigado", "Oi")

        assertTrue(moves.isEmpty())
    }
}
