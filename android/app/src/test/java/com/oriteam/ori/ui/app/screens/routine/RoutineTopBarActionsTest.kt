package com.oriteam.ori.ui.app.screens.routine

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.testing.bounds
import com.oriteam.ori.ui.components.state.ContentState
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.OriTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * What a screen puts at the end of the title row of the routine: the panel of ORI Home puts "Abrir no ORI" there, so
 * the panel has one bar. It had two: a row with only "Abrir no ORI" and, under it, the title of the routine.
 */
@RunWith(AndroidJUnit4::class)
class RoutineTopBarActionsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `an action given to the routine goes in its title row`() {
        composeRule.setContent {
            OriTheme {
                RoutineDayList(
                    state = RoutineUiState(date = LocalDate.of(2026, 11, 4), content = ContentState.Empty),
                    title = "Minha rotina",
                    selectedId = null,
                    emptyMessage = "Nada",
                    onNavigateBack = null,
                    onSelectDate = {},
                    onToggleDone = {},
                    onOpen = {},
                    onCreate = {},
                    onRetry = {},
                    topBarActions = { Text("Abrir no ORI") }
                )
            }
        }

        val title = composeRule.onNodeWithText("Minha rotina").bounds
        val action = composeRule.onNodeWithText("Abrir no ORI").bounds
        val middle = action.top + action.height / 2
        assertTrue("action at $middle, title from ${title.top} to ${title.bottom}", middle in title.top..title.bottom)
    }
}
