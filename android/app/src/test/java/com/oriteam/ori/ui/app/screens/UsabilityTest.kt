package com.oriteam.ori.ui.app.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.domain.aac.SpeechState
import com.oriteam.ori.domain.routine.RepeatRule
import com.oriteam.ori.testing.DAY
import com.oriteam.ori.testing.OriTestWindow
import com.oriteam.ori.testing.bounds
import com.oriteam.ori.ui.app.screens.aac.AacMessageBar
import com.oriteam.ori.ui.app.screens.aac.AacMessageBarActions
import com.oriteam.ori.ui.app.screens.aac.AacSymbolUiModel
import com.oriteam.ori.ui.app.screens.routine.ActivityTimeField
import com.oriteam.ori.ui.app.screens.routine.DayCell
import com.oriteam.ori.ui.app.screens.routine.RepeatPicker
import com.oriteam.ori.ui.app.screens.routine.RoutineUiState
import com.oriteam.ori.ui.app.screens.routine.TasksScreen
import com.oriteam.ori.ui.components.navigation.OpenSubscreen
import com.oriteam.ori.ui.components.navigation.OriFilterTab
import com.oriteam.ori.ui.components.navigation.OriFilterTabs
import com.oriteam.ori.ui.components.navigation.rememberOpenSubscreen
import com.oriteam.ori.ui.components.text.Text
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek

/** What a person meets when using the routine and the communication, on a 360 dp phone. */
@RunWith(AndroidJUnit4::class)
class UsabilityTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun render(fontScale: Float = 1f, content: @Composable () -> Unit) {
        composeRule.setContent {
            OriTestWindow(
                widthDp = 360,
                heightDp = 780,
                fontScale = fontScale,
                content = content
            )
        }
    }

    @Test
    fun `the time can be chosen on a clock instead of typed`() {
        var time = ""
        render { ActivityTimeField(timeInput = "", onTimeChange = { time = it }) }

        composeRule.onNodeWithText("Escolher").performClick()
        composeRule.onNodeWithText("Usar este horário").performClick()

        assertEquals("08:00", time)
    }

    @Test
    fun `each day of the repetition has the whole height of a touch target`() {
        render { RepeatPicker(rule = RepeatRule.Weekly(setOf(DayOfWeek.MONDAY)), startDate = DAY, onRuleChange = {}) }

        val monday = composeRule.onNodeWithContentDescription("segunda-feira").bounds
        assertTrue(monday.bottom - monday.top >= 48.dp)
        assertTrue("an equal share of the row, wider than the 40 dp circle", monday.right - monday.left > 40.dp)
    }

    @Test
    fun `a faded day of another month says its month`() {
        render {
            DayCell(
                day = DAY.withDayOfMonth(1).plusMonths(1),
                isSelected = false,
                isToday = false,
                onClick = {},
                isDimmed = true
            )
        }

        composeRule.onNodeWithContentDescription("1 de novembro", substring = true).assertExists()
    }

    @Test
    fun `a day with activities says so, besides the dot`() {
        render { DayCell(day = DAY, isSelected = false, isToday = false, onClick = {}, hasActivities = true) }

        composeRule.onNodeWithContentDescription("tem atividades", substring = true).assertExists()
    }

    @Test
    fun `with very large text every tab is still on the screen`() {
        render(fontScale = 2f) {
            OriFilterTabs(
                tabs = listOf(OriFilterTab(1, "Pendentes"), OriFilterTab(2, "Concluídas"), OriFilterTab(3, "Todas")),
                selected = 1,
                onSelect = {}
            )
        }

        composeRule.onNodeWithText("Todas").assertIsDisplayed()
        assertTrue(composeRule.onNodeWithText("Todas").bounds.right <= 360.dp)
    }

    @Test
    fun `tasks say which day they are`() {
        render { TasksScreen(RoutineUiState(date = DAY), {}, {}, {}, {}) }

        composeRule.onNodeWithText("Hoje, quinta-feira, 29 de outubro").assertIsDisplayed()
    }

    @Test
    fun `back closes the open sub-screen first`() {
        lateinit var subscreen: OpenSubscreen<String>
        render {
            subscreen = rememberOpenSubscreen()
            Text(text = subscreen.current ?: "área")
        }
        composeRule.runOnIdle { subscreen.open("subtela") }
        composeRule.onNodeWithText("subtela").assertExists()

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }

        composeRule.onNodeWithText("área").assertExists()
    }

    @Test
    fun `a long message shows that earlier words scrolled away`() {
        val words = List(12) { AacSymbolUiModel(id = "$it", label = "palavra$it") }
        render {
            Column { AacMessageBar(words, SpeechState.READY, isFull = false, actions = AacMessageBarActions.None) }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("…").assertIsDisplayed()
    }
}
