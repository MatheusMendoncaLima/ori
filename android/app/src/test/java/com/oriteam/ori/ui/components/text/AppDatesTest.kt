package com.oriteam.ori.ui.components.text

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.Locale

private val SATURDAY = LocalDate.of(2026, 11, 7)
private val BRAZIL = Locale.forLanguageTag("pt-BR")
private val SPAIN = Locale.forLanguageTag("es-ES")

/** Each language puts the same date in its own order and words; nothing is written by hand. */
@RunWith(AndroidJUnit4::class)
class AppDatesTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun format(skeleton: String, locale: Locale) = SATURDAY.format(dateFormatFor(skeleton, locale))

    @Test
    fun `weekday, day and month read naturally in each language`() {
        assertEquals("sábado, 7 de novembro", format(DateSkeletons.WEEKDAY_DAY_MONTH, BRAZIL))
        assertEquals("Saturday, November 7", format(DateSkeletons.WEEKDAY_DAY_MONTH, Locale.US))
        assertEquals("sábado, 7 de noviembre", format(DateSkeletons.WEEKDAY_DAY_MONTH, SPAIN))
    }

    @Test
    fun `day and month in numbers follow the order of each language`() {
        assertEquals("07/11", format(DateSkeletons.DAY_MONTH_NUMBERS, BRAZIL))
        assertEquals("11/07", format(DateSkeletons.DAY_MONTH_NUMBERS, Locale.US))
    }

    @Test
    fun `the month title has the words of each language`() {
        assertEquals("novembro de 2026", format(DateSkeletons.MONTH_YEAR, BRAZIL))
        assertEquals("November 2026", format(DateSkeletons.MONTH_YEAR, Locale.US))
    }

    @Test
    fun `a screen writes dates in the language it is given, and in Portuguese by default`() {
        composeRule.setContent {
            Text(SATURDAY.format(rememberDateFormat(DateSkeletons.WEEKDAY_DAY_MONTH)))
            CompositionLocalProvider(LocalAppLocale provides Locale.US) {
                Text(SATURDAY.format(rememberDateFormat(DateSkeletons.WEEKDAY_DAY_MONTH)))
            }
        }

        composeRule.onNodeWithText("sábado, 7 de novembro").assertExists()
        composeRule.onNodeWithText("Saturday, November 7").assertExists()
    }
}
