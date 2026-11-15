package com.oriteam.ori.ui.components.dialog

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.testing.OriTestWindow
import com.oriteam.ori.ui.components.text.Text
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private val LocalScreenName = compositionLocalOf { "host" }

/**
 * The dialogs of ORI are drawn in the window of the screen (a window of their own cost 50 to 90 ms at each opening on
 * the test tablet), and still behave as a dialog window: read as a dialog, the screen under it out of reach of a
 * finger and of the screen reader, closed by back or by a touch outside.
 */
@RunWith(AndroidJUnit4::class)
class OriDialogLayerTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var isOpen by mutableStateOf(true)
    private var screenTouches by mutableIntStateOf(0)
    private var screenBacks by mutableIntStateOf(0)
    private var count by mutableIntStateOf(1)

    private fun render(dialog: @Composable () -> Unit = { Question() }) {
        composeRule.setContent {
            OriTestWindow(widthDp = 360, heightDp = 640) {
                BackHandler { screenBacks++ }
                Box(modifier = Modifier.fillMaxSize().clickable { screenTouches++ }) { Text("Tela atrás") }
                if (isOpen) dialog()
            }
        }
        composeRule.waitForIdle()
    }

    @Composable
    private fun Question() {
        OriAlertDialog(
            onDismissRequest = { isOpen = false },
            title = "Apagar a atividade?",
            text = { Text("Contagem $count") },
            confirmButton = { TextButton(onClick = { isOpen = false }) { Text("Apagar") } },
            dismissButton = { TextButton(onClick = { isOpen = false }) { Text("Cancelar") } }
        )
    }

    @Test
    fun `a dialog opens in the window of the screen and is read as a dialog with its title`() {
        render()

        composeRule.onAllNodes(isRoot()).assertCountEquals(1)
        composeRule.onNode(isDialog()).assertExists()
        composeRule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Apagar a atividade?"))
            .assertExists()
        composeRule.onNodeWithText("Apagar").assertExists()
    }

    @Test
    fun `the screen under an open dialog cannot be read, and can again once it closes`() {
        render()
        composeRule.onNodeWithText("Tela atrás").assertDoesNotExist()

        composeRule.onNodeWithText("Cancelar").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Tela atrás").assertExists()
    }

    @Test
    fun `a touch outside closes the dialog and does not reach the screen under it`() {
        render()

        composeRule.onRoot().performTouchInput { click(Offset(5f, 5f)) }
        composeRule.waitForIdle()

        assertEquals(false, isOpen)
        assertEquals(0, screenTouches)
    }

    @Test
    fun `a touch on the dialog itself keeps it open`() {
        render()

        composeRule.onNodeWithText("Contagem 1").performTouchInput { click() }
        composeRule.waitForIdle()

        assertEquals(true, isOpen)
        assertEquals(0, screenTouches)
    }

    @Test
    fun `back closes the dialog and not the screen`() {
        render()

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        assertEquals(false, isOpen)
        assertEquals(0, screenBacks)
    }

    @Test
    fun `the dialog shows what the screen has now`() {
        render()

        count = 2
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Contagem 2").assertExists()
    }

    @Test
    fun `the dialog is drawn with the values of the screen that asked for it`() {
        render {
            CompositionLocalProvider(LocalScreenName provides "screen") {
                OriDialog(onDismissRequest = { isOpen = false }, paneTitle = "Nome") {
                    Text("Valor: ${LocalScreenName.current}")
                }
            }
        }

        composeRule.onNodeWithText("Valor: screen").assertExists()
    }

    @Test
    fun `a dialog of the whole screen is not closed by a touch on it`() {
        render {
            OriDialog(
                onDismissRequest = { isOpen = false },
                paneTitle = "Mensagem",
                placement = DialogPlacement.FULL_SCREEN
            ) { Box(modifier = Modifier.fillMaxSize()) { Text("Mensagem grande") } }
        }

        composeRule.onRoot().performTouchInput { click(Offset(5f, 5f)) }
        composeRule.waitForIdle()

        assertEquals(true, isOpen)
        assertEquals(0, screenTouches)
    }
}
