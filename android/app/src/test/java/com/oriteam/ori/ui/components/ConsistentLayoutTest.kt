package com.oriteam.ori.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.domain.reports.DayResult
import com.oriteam.ori.domain.reports.RoutineReport
import com.oriteam.ori.domain.routine.RepeatRule
import com.oriteam.ori.testing.DAY
import com.oriteam.ori.testing.OriTestWindow
import com.oriteam.ori.testing.bounds
import com.oriteam.ori.ui.adaptive.CenteredContent
import com.oriteam.ori.ui.app.screens.aac.AacOrganizeUiState
import com.oriteam.ori.ui.app.screens.aac.AacSelection
import com.oriteam.ori.ui.app.screens.aac.BoardSelector
import com.oriteam.ori.ui.app.screens.aac.BoardTabUiModel
import com.oriteam.ori.ui.app.screens.aac.BoardsPane
import com.oriteam.ori.ui.app.screens.aac.OrganizerItemUiModel
import com.oriteam.ori.ui.app.screens.reports.ReportContent
import com.oriteam.ori.ui.app.screens.routine.DayPart
import com.oriteam.ori.ui.app.screens.routine.RepeatPicker
import com.oriteam.ori.ui.app.screens.routine.RoutineStepUiModel
import com.oriteam.ori.ui.app.screens.routine.dayPartOf
import com.oriteam.ori.ui.components.navigation.OriTopBar
import com.oriteam.ori.ui.components.state.ContentState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalTime

/** On a 360 dp phone the content margin is 24 dp; titles, buttons and lists start there. */
@RunWith(AndroidJUnit4::class)
class ConsistentLayoutTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val margin = 24.dp

    private fun render(content: @Composable () -> Unit) {
        composeRule.setContent { OriTestWindow(widthDp = 360, heightDp = 780, content = content) }
    }

    @Test
    fun `the title of a screen starts at the content margin, and so does the back arrow`() {
        render {
            Column {
                OriTopBar(title = "Minha rotina")
                OriTopBar(title = "Tarefas", onNavigateBack = {})
            }
        }

        assertEquals(margin, composeRule.onNodeWithText("Minha rotina", useUnmergedTree = true).bounds.left)
        val arrow = composeRule.onNodeWithContentDescription("Voltar", useUnmergedTree = true)
        assertEquals(margin, arrow.bounds.left)
    }

    @Test
    fun `organizing boards uses the same margin as the other screens`() {
        val boards = listOf(OrganizerItemUiModel(BoardTabUiModel("a", "Básico"), false, false))
        render {
            BoardsPane(
                AacOrganizeUiState(canManage = true, boards = ContentState.Content(boards)),
                "Organizar pranchas",
                null,
                {},
                { _, _ -> },
                {},
                {}
            )
        }

        assertEquals(margin, composeRule.onNodeWithText("Nova prancha").bounds.left)
        assertEquals(margin, composeRule.onNodeWithText("Básico").bounds.left)
    }

    @Test
    fun `a section title starts at the margin instead of being centered`() {
        render {
            CenteredContent { ReportContent(RoutineReport(listOf(DayResult(DAY, 1, 2)), 3)) }
        }

        assertEquals(margin, composeRule.onNodeWithText("Resumo geral").bounds.left)
    }

    @Test
    fun `options of a form are one choice among many, and boards are tabs`() {
        render {
            Column {
                RepeatPicker(rule = RepeatRule.LastDayOfMonth, startDate = DAY, onRuleChange = {})
                BoardSelector(
                    boards = listOf(BoardTabUiModel("a", "Básico"), BoardTabUiModel("b", "Comida")),
                    selection = AacSelection.Board("a"),
                    onSelect = {}
                )
            }
        }

        composeRule.onNodeWithText("Último dia do mês").assertIsSelected().assert(hasRole(Role.RadioButton))
        composeRule.onNodeWithText("Não repete").assertIsNotSelected().assert(hasRole(Role.RadioButton))
        composeRule.onNodeWithText("Básico").assertIsSelected().assert(hasRole(Role.Tab))
    }

    @Test
    fun `an activity keeps its color in any list and on every day it repeats, and the color says when it is`() {
        val monday =
            RoutineStepUiModel(id = "s__2026-11-02", title = "Terapia", time = LocalTime.of(14, 0), seriesId = "s")
        val nextMonday = monday.copy(id = "s__2026-11-09")
        val breakfast = RoutineStepUiModel(id = "b", title = "Café", time = LocalTime.of(8, 0))

        assertEquals(dayPartOf(monday.time), dayPartOf(nextMonday.time))
        assertEquals(DayPart.AFTERNOON, dayPartOf(monday.time))
        assertEquals(DayPart.MORNING, dayPartOf(breakfast.time))
    }

    private fun hasRole(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)
}
