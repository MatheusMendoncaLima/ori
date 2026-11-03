package com.oriteam.ori.ui.app.screens.routine

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.domain.routine.FIRST_DAY_OF_WEEK
import com.oriteam.ori.domain.routine.RepeatRule
import com.oriteam.ori.ui.components.state.ContentState
import com.oriteam.ori.ui.theme.OriTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class WeekSwipeAndRepeatTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val wednesday = LocalDate.of(2026, 11, 4)

    private fun renderWeek(): () -> LocalDate {
        var date by mutableStateOf(wednesday)
        composeRule.setContent {
            OriTheme { WeekStrip(date = date, today = wednesday, onSelectDate = { date = it }) }
        }
        return { date }
    }

    @Test
    fun `dragging changes the week and keeps the day of the week`() {
        val date = renderWeek()

        composeRule.onNodeWithText("Novembro 2026").assertExists()
        composeRule.onNodeWithContentDescription("quarta-feira, dia 4, hoje", substring = true)
            .performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        assertEquals(wednesday.plusWeeks(1), date())

        composeRule.onNodeWithContentDescription("quarta-feira, dia 11", substring = true)
            .performTouchInput { swipeRight() }
        composeRule.waitForIdle()
        assertEquals(wednesday, date())
    }

    @Test
    fun `the arrows still work and move the days shown`() {
        val date = renderWeek()

        composeRule.onNodeWithContentDescription(composeRule.activity.getString(com.oriteam.ori.R.string.week_next))
            .performClick()
        composeRule.waitForIdle()

        assertEquals(wednesday.plusWeeks(1), date())
        composeRule.onNodeWithContentDescription("quarta-feira, dia 11", substring = true).assertExists()
    }

    private fun renderDayList(): () -> LocalDate {
        var date by mutableStateOf(wednesday)
        composeRule.setContent {
            OriTheme {
                RoutineDayList(
                    state = RoutineUiState(date = date, content = ContentState.Empty),
                    title = "Minha rotina",
                    selectedId = null,
                    emptyMessage = "Nada",
                    onNavigateBack = null,
                    onSelectDate = { date = it },
                    onToggleDone = {},
                    onOpen = {},
                    onCreate = {},
                    onRetry = {}
                )
            }
        }
        return { date }
    }

    private val lastDayOfTheMonth get() = composeRule.onNodeWithContentDescription(
        "segunda-feira, dia 30",
        substring = true
    )

    @Test
    fun `dragging down opens the whole month, and dragging up goes back to the week`() {
        renderDayList()
        lastDayOfTheMonth.assertDoesNotExist()

        // Over the calendar.
        composeRule.onNodeWithText("Novembro 2026").performTouchInput {
            swipeDown(startY = centerY, endY = centerY + 200f)
        }
        composeRule.waitForIdle()
        lastDayOfTheMonth.assertExists()

        composeRule.onNodeWithText("Novembro 2026").performTouchInput {
            swipeUp(startY = centerY + 200f, endY = centerY)
        }
        composeRule.waitForIdle()
        lastDayOfTheMonth.assertDoesNotExist()
    }

    @Test
    fun `dragging down over the day also opens the month, and a small drag changes nothing`() {
        renderDayList()

        composeRule.onRoot().performTouchInput { swipeDown(startY = centerY, endY = centerY + 20f) }
        composeRule.waitForIdle()
        lastDayOfTheMonth.assertDoesNotExist()

        composeRule.onRoot().performTouchInput { swipeDown(startY = centerY, endY = bottom - 10f) }
        composeRule.waitForIdle()
        lastDayOfTheMonth.assertExists()
    }

    @Test
    fun `the bar does the same with a tap, for who cannot drag`() {
        renderDayList()

        composeRule.onNodeWithContentDescription("Ver o mês inteiro").performClick()
        lastDayOfTheMonth.assertExists()

        composeRule.onNodeWithContentDescription("Voltar para a semana").performClick()
        lastDayOfTheMonth.assertDoesNotExist()
    }

    @Test
    fun `in the month, choosing a day and changing the month keep working`() {
        val date = renderDayList()
        composeRule.onNodeWithContentDescription("Ver o mês inteiro").performClick()

        lastDayOfTheMonth.performClick()
        assertEquals(LocalDate.of(2026, 11, 30), date())
        composeRule.onNodeWithContentDescription("Próximo mês").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Dezembro 2026").assertExists()
    }

    @Test
    fun `the month always shows six weeks starting on the first day of the week`() {
        val days = daysShownIn(java.time.YearMonth.of(2026, 12))

        assertEquals(42, days.size)
        assertEquals(FIRST_DAY_OF_WEEK, days.first().dayOfWeek)
        assertTrue(days.first() <= LocalDate.of(2026, 12, 1))
        assertTrue(LocalDate.of(2026, 12, 31) in days)
    }

    @Test
    fun `weeks start on Sunday, as Brazilian calendars do`() {
        assertEquals(DayOfWeek.SUNDAY, FIRST_DAY_OF_WEEK)
        assertEquals(LocalDate.of(2026, 11, 1), weekOf(wednesday).first())
        val days = daysShownIn(java.time.YearMonth.of(2026, 11))
        assertEquals(true, LocalDate.of(2026, 11, 30) in days)
    }

    @Test
    fun `a form without its date yet draws nothing instead of crashing`() {
        composeRule.setContent {
            OriTheme {
                ActivityEditorScreen(
                    state = ActivityEditorUiState(),
                    today = wednesday,
                    onTitleChange = {},
                    onTimeChange = {},
                    onDescriptionChange = {},
                    onDateChange = {},
                    onOwnerCanCompleteChange = {},
                    onHiddenFromCaregiverChange = {},
                    onSave = {},
                    onClose = {}
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `calendars never crash at the edges of time`() {
        composeRule.setContent {
            OriTheme {
                androidx.compose.foundation.layout.Column {
                    WeekStrip(date = LocalDate.of(1, 1, 1), today = wednesday, onSelectDate = {})
                    RoutineCalendar(
                        date = LocalDate.of(9999, 12, 31),
                        today = wednesday,
                        isMonthOpen = true,
                        onSelectDate = {},
                        onMonthOpenChange = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `the circles choose days and the options replace them`() {
        var rule by mutableStateOf<RepeatRule?>(null)
        composeRule.setContent {
            OriTheme { RepeatPicker(rule = rule, startDate = wednesday, onRuleChange = { rule = it }) }
        }

        composeRule.onNodeWithContentDescription("segunda-feira").performClick()
        composeRule.onNodeWithContentDescription("quarta-feira").performClick()
        assertEquals(RepeatRule.Weekly(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)), rule)
        composeRule.onNodeWithText("Toda semana: seg, qua, a partir de 4/11").assertExists()
        composeRule.onNodeWithContentDescription("domingo").performClick()
        composeRule.onNodeWithText("Toda semana: dom, seg, qua, a partir de 4/11").assertExists()
        composeRule.onNodeWithContentDescription("domingo").performClick()

        composeRule.onNodeWithText("2 semanas").performClick()
        assertEquals(RepeatRule.Weekly(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), 2), rule)

        composeRule.onNodeWithText("Último dia do mês").performClick()
        assertEquals(RepeatRule.LastDayOfMonth, rule)

        composeRule.onNodeWithText("Não repete").performClick()
        assertNull(rule)
        composeRule.onNodeWithText("Só no dia 4/11").assertExists()
    }
}
