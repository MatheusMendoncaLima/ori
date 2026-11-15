package com.oriteam.ori.ui.launcher

import com.oriteam.ori.domain.launcher.HomeSpan
import com.oriteam.ori.testing.widgetProvider
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * What tells apart widgets of one app with the same name and size (five "Current weather and forecast", 2 × 2, looked
 * the same on the tablet): their description when each has its own, a number otherwise; nothing for the others.
 */
class WidgetVariantTest {
    @Test
    fun `widgets with their own descriptions are told apart by them`() {
        val a = widgetProvider("Tempo", id = "a", description = "Hoje")
        val b = widgetProvider("Tempo", id = "b", description = "Semana")
        val other = widgetProvider("Notas", id = "c")

        val variants = variantsOf(listOf(a, b, other))

        assertEquals(
            mapOf("a" to WidgetVariant.Described("Hoje"), "b" to WidgetVariant.Described("Semana")),
            variants
        )
    }

    @Test
    fun `without a description, or with the same one, they are numbered in the order of the list`() {
        val a = widgetProvider("Tempo", id = "a", description = "Previsão")
        val b = widgetProvider("Tempo", id = "b", description = "Previsão")
        val c = widgetProvider("Tempo", id = "c")

        assertEquals(
            mapOf(
                "a" to WidgetVariant.Numbered(1),
                "b" to WidgetVariant.Numbered(2),
                "c" to WidgetVariant.Numbered(3)
            ),
            variantsOf(listOf(a, b, c))
        )
    }

    @Test
    fun `the same name in another size needs no note`() {
        val small = widgetProvider("Tempo", id = "a", span = HomeSpan(2, 1))
        val big = widgetProvider("Tempo", id = "b", span = HomeSpan(4, 2))

        assertEquals(emptyMap<String, WidgetVariant>(), variantsOf(listOf(small, big)))
    }
}
