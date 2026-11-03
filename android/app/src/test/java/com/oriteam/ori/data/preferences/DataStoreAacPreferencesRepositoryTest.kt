package com.oriteam.ori.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.oriteam.ori.domain.preferences.AacPreferences
import com.oriteam.ori.domain.preferences.MessageMode
import com.oriteam.ori.domain.preferences.SymbolSize
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Each test writes its file once: on a Windows computer the JVM DataStore cannot replace an
 * existing file (Android devices can), so a second write would fail only on the test machine.
 */
class DataStoreAacPreferencesRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun TestScope.dataStore(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = backgroundScope) { File(folder.root, "aac.preferences_pb") }

    @Test
    fun `defaults first, then the saved choices`() = runTest {
        val repository = DataStoreAacPreferencesRepository(dataStore())
        assertEquals(AacPreferences(), repository.preferences.first())

        repository.update { it.copy(symbolSize = SymbolSize.LARGE, messageMode = MessageMode.SPEAK_IMMEDIATELY) }

        assertEquals(
            AacPreferences(symbolSize = SymbolSize.LARGE, messageMode = MessageMode.SPEAK_IMMEDIATELY),
            repository.preferences.first()
        )
    }

    @Test
    fun `an unknown stored value falls back to the default instead of crashing`() = runTest {
        val dataStore = dataStore()
        // A value written by a newer app version.
        dataStore.edit { it[stringPreferencesKey("aac_symbol_size")] = "HUGE" }

        assertEquals(SymbolSize.SMALL, DataStoreAacPreferencesRepository(dataStore).preferences.first().symbolSize)
    }
}
