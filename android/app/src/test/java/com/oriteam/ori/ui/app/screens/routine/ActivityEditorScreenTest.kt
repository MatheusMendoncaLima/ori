package com.oriteam.ori.ui.app.screens.routine

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.testing.OriTestWindow
import com.oriteam.ori.testing.bounds
import com.oriteam.ori.ui.components.form.showsActionBar
import com.oriteam.ori.ui.theme.OriTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** How the activity form ends and how "Repetir" explains itself (review items I3 and I4). */
@RunWith(AndroidJUnit4::class)
class ActivityEditorScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val wednesday = LocalDate.of(2026, 11, 4)
    private var saves = 0
    private var closes = 0

    @Test
    fun `save and cancel are on a small phone screen without scrolling`() {
        composeRule.setContent {
            OriTestWindow(widthDp = 360, heightDp = 640) {
                ActivityEditorScreen(
                    state = ActivityEditorUiState(titleInput = "Tomar remédio", date = wednesday),
                    today = wednesday,
                    onTitleChange = {},
                    onTimeChange = {},
                    onDescriptionChange = {},
                    onDateChange = {},
                    onOwnerCanCompleteChange = {},
                    onHiddenFromCaregiverChange = {},
                    onSave = { saves++ },
                    onClose = { closes++ }
                )
            }
        }

        composeRule.onNodeWithText("Salvar").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Cancelar").assertIsDisplayed().performClick()

        assertEquals(1, saves)
        assertEquals(1, closes)
    }

    private fun showEditorWithChanges() {
        composeRule.setContent {
            OriTestWindow(widthDp = 360, heightDp = 640) {
                ActivityEditorScreen(
                    state = ActivityEditorUiState(titleInput = "Tomar remédio", date = wednesday, hasChanges = true),
                    today = wednesday,
                    onTitleChange = {},
                    onTimeChange = {},
                    onDescriptionChange = {},
                    onDateChange = {},
                    onOwnerCanCompleteChange = {},
                    onHiddenFromCaregiverChange = {},
                    onSave = { saves++ },
                    onClose = { closes++ }
                )
            }
        }
    }

    @Test
    fun `cancelling what was typed asks first, and keeping on editing loses nothing`() {
        showEditorWithChanges()

        composeRule.onNodeWithText("Cancelar").performClick()
        composeRule.onNodeWithText("Descartar as mudanças?").assertIsDisplayed()
        composeRule.onNodeWithText("Continuar editando").performClick()
        assertEquals(0, closes)
        composeRule.onNodeWithText("Descartar as mudanças?").assertDoesNotExist()

        composeRule.onNodeWithText("Cancelar").performClick()
        composeRule.onNodeWithText("Descartar").performClick()
        assertEquals(1, closes)
    }

    @Test
    fun `the system back with something typed asks too, instead of closing the form`() {
        showEditorWithChanges()

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }

        composeRule.onNodeWithText("Descartar as mudanças?").assertIsDisplayed()
        assertEquals(0, closes)
    }

    @Test
    fun `the action bar waits for the keyboard only when the form is short`() {
        assertTrue(showsActionBar(isKeyboardOpen = false, formHeight = 200.dp))
        assertTrue(showsActionBar(isKeyboardOpen = true, formHeight = 560.dp))
        // The tablet lying down with the keyboard open: about 390 dp were left, and "Cancelar" took the time field.
        assertFalse(showsActionBar(isKeyboardOpen = true, formHeight = 390.dp))
    }

    @Test
    fun `the days of the week come after the options, under a label that says what they do`() {
        composeRule.setContent {
            OriTheme { RepeatPicker(rule = null, startDate = wednesday, onRuleChange = {}) }
        }

        val options = composeRule.onNodeWithText("Não repete").bounds
        val label = composeRule.onNodeWithText("Ou escolha os dias da semana").bounds
        val monday = composeRule.onNodeWithContentDescription("segunda-feira").bounds

        assertTrue("options before the label", options.bottom <= label.top)
        assertTrue("label before the days", label.bottom <= monday.top)
    }
}
