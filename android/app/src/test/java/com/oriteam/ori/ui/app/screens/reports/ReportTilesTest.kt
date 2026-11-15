package com.oriteam.ori.ui.app.screens.reports

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.domain.reports.DayResult
import com.oriteam.ori.domain.reports.RoutineReport
import com.oriteam.ori.testing.OriTestWindow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/** The defect: "Do planejado" fits in one line and its tile was shorter than the others. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReportTilesTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `the summary tiles have the same height whatever their labels say`() {
        composeRule.setContent {
            OriTestWindow(widthDp = 360, heightDp = 780) {
                ReportContent(RoutineReport(listOf(DayResult(LocalDate.of(2026, 11, 2), 1, 2)), speechCount = 3))
            }
        }

        val heights = listOf("Atividade concluída", "Do planejado", "Vezes que se comunicou").map { label ->
            composeRule.onNodeWithText(label, substring = true).fetchSemanticsNode().boundsInRoot.height
        }

        assertEquals(heights.first(), heights[1], 1f)
        assertEquals(heights.first(), heights[2], 1f)
    }

    @Test
    fun `each count says its label in the singular or the plural, as its number asks`() {
        composeRule.setContent {
            OriTestWindow(widthDp = 360, heightDp = 780) {
                ReportContent(RoutineReport(listOf(DayResult(LocalDate.of(2026, 11, 2), 3, 4)), speechCount = 1))
            }
        }

        composeRule.onNodeWithText("Atividades concluídas").assertExists()
        composeRule.onNodeWithText("Vez que se comunicou").assertExists()
    }
}
