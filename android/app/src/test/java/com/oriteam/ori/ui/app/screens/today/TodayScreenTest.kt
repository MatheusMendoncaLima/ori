package com.oriteam.ori.ui.app.screens.today

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.R
import com.oriteam.ori.ui.app.screens.routine.RoutineStepUiModel
import com.oriteam.ori.ui.components.state.ContentState
import com.oriteam.ori.ui.theme.OriTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalTime

/** Only a successful load may tell the person that the day has no activities. */
@RunWith(AndroidJUnit4::class)
class TodayScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val noActivities get() = composeRule.activity.getString(R.string.today_empty)
    private var retries = 0
    private var routineOpenings = 0

    private fun render(state: ContentState<List<RoutineStepUiModel>>) {
        composeRule.setContent {
            OriTheme {
                TodayScreen(
                    firstName = "Ana",
                    steps = state,
                    now = LocalTime.of(10, 0),
                    onRetry = { retries++ },
                    onOpenRoutine = { routineOpenings++ },
                    onOpenCommunication = {},
                    onOpenSubscreen = {}
                )
            }
        }
    }

    @Test
    fun `the greeting says the first name`() {
        render(ContentState.Empty)

        composeRule.onNodeWithText("Olá, Ana!").assertExists()
    }

    @Test
    fun `while loading it does not say the day is empty`() {
        render(ContentState.Loading)

        composeRule.onNodeWithText(noActivities).assertDoesNotExist()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.state_loading)).assertExists()
    }

    @Test
    fun `a failure shows the error and can be retried, and does not say the day is empty`() {
        render(ContentState.Error(R.string.state_error_title))

        composeRule.onNodeWithText(noActivities).assertDoesNotExist()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.state_error_title)).assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.action_retry)).performClick()
        assertEquals(1, retries)
    }

    @Test
    fun `an empty day says so and leads to the routine`() {
        render(ContentState.Empty)

        composeRule.onNodeWithText(noActivities).assertExists()
        composeRule.onNodeWithText("Toque para planejar o dia em \"Rotina\".").performClick()
        assertEquals(1, routineOpenings)
    }

    @Test
    fun `a day with activities shows the next one`() {
        render(ContentState.Content(listOf(RoutineStepUiModel("1", "Terapia", LocalTime.of(14, 0)))))

        composeRule.onNodeWithText("14:00 · Terapia").assertExists()
    }

    @Test
    fun `an activity whose time has passed is not the next one, it is counted apart`() {
        render(
            ContentState.Content(
                listOf(
                    RoutineStepUiModel("1", "Café", LocalTime.of(8, 0)),
                    RoutineStepUiModel("2", "Terapia", LocalTime.of(14, 0))
                )
            )
        )

        composeRule.onNodeWithText("14:00 · Terapia").assertExists()
        composeRule.onNodeWithText("08:00 · Café").assertDoesNotExist()
        composeRule.onNodeWithText("1 atividade de mais cedo ainda não foi feita").assertExists()
    }
}
