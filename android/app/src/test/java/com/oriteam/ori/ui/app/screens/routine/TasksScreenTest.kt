package com.oriteam.ori.ui.app.screens.routine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.testing.DAY
import com.oriteam.ori.testing.OriTestWindow
import com.oriteam.ori.ui.components.state.ContentState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TasksScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `a task marked under Pendentes stays a moment, so a mistaken tap can be undone, and then leaves`() {
        var steps by mutableStateOf(listOf(RoutineStepUiModel("b", "Remédio")))
        composeRule.setContent {
            OriTestWindow(widthDp = 360, heightDp = 800) {
                TasksScreen(
                    state = RoutineUiState(date = DAY, content = ContentState.Content(steps)),
                    onToggleDone = { id ->
                        steps = steps.map { if (it.id == id) it.copy(isDone = !it.isDone) else it }
                    },
                    onCreate = {},
                    onRetry = {},
                    onNavigateBack = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription("Concluir: Remédio").performClick()
        composeRule.onNodeWithText("Remédio").assertExists()

        composeRule.mainClock.advanceTimeBy(KEEP_JUST_DONE_MILLIS + 1)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Remédio").assertDoesNotExist()
    }
}
