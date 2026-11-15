package com.oriteam.ori.ui.launcher

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The answers of the widget windows wait for the home screen, which is stopped while a window shows: none is lost,
 * they come in order, and each is read once.
 */
class WidgetStepResultsTest {
    @Test
    fun `answers sent while nobody reads wait, in order, and are read once`() = runTest {
        val results = WidgetStepResults()
        val bind = WidgetStepResult(WidgetStep.BIND, widgetId = 3, isDone = true)
        val setUp = WidgetStepResult(WidgetStep.SET_UP, widgetId = 3, isDone = false)

        results.send(bind)
        results.send(setUp)

        assertEquals(listOf(bind, setUp), results.results.take(2).toList())
        results.send(bind)
        assertEquals(bind, results.results.first())
    }
}
