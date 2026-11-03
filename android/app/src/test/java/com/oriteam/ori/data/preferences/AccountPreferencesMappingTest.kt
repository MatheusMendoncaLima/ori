package com.oriteam.ori.data.preferences

import com.oriteam.ori.data.firestore.FirestoreSchema
import com.oriteam.ori.domain.preferences.AccountPreferences
import com.oriteam.ori.domain.preferences.MotionLevel
import com.oriteam.ori.domain.preferences.TextScale
import com.oriteam.ori.domain.preferences.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class AccountPreferencesMappingTest {

    @Test
    fun `every combination of values survives writing and reading`() {
        TextScale.entries.forEach { textScale ->
            ThemeMode.entries.forEach { theme ->
                MotionLevel.entries.forEach { motion ->
                    val preferences = AccountPreferences(textScale, theme, isSimplified = true, motion = motion)
                    assertEquals(preferences, accountPreferencesFrom(preferences.toDocument()))
                }
            }
        }
    }

    @Test
    fun `an unknown value falls back alone, keeping the other choices`() {
        val data = AccountPreferences(theme = ThemeMode.DARK, isSimplified = true).toDocument() +
            (FirestoreSchema.TEXT_SCALE to "from-a-newer-version")

        assertEquals(
            AccountPreferences(textScale = TextScale.NORMAL, theme = ThemeMode.DARK, isSimplified = true),
            accountPreferencesFrom(data)
        )
    }

    @Test
    fun `the document has exactly the fields the rules require`() {
        assertEquals(
            setOf("textScale", "theme", "simplified", "motion", "updatedAt"),
            AccountPreferences().toDocument().keys
        )
    }
}
