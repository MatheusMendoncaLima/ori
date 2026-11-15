package com.oriteam.ori.ui.app.screens.routine

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.testing.OriTestWindow
import com.oriteam.ori.testing.bounds
import com.oriteam.ori.ui.theme.OriTheme
import org.junit.Assert.assertEquals
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
