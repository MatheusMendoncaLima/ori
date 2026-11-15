package com.oriteam.ori.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.oriteam.ori.domain.preferences.AacPreferences
import com.oriteam.ori.domain.preferences.MAX_VOICE_PITCH
import com.oriteam.ori.domain.preferences.MessageMode
import com.oriteam.ori.domain.preferences.SymbolSize
import com.oriteam.ori.domain.preferences.VoiceSettings
import com.oriteam.ori.domain.preferences.VoiceSound
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
    fun `the voice, its tone, its speed and its volume are kept, inside the range engines understand`() = runTest {
        val repository = DataStoreAacPreferencesRepository(dataStore())
        val chosen = VoiceSettings(voiceId = "pt-br-x-afs-local", pitch = 9f, rate = 1.1f, sound = VoiceSound.RING)

        repository.update { it.copy(voice = chosen) }

        assertEquals(chosen.copy(pitch = MAX_VOICE_PITCH), repository.preferences.first().voice)
    }

    @Test
    fun `the order of the ready-made phrases is kept`() = runTest {
        val repository = DataStoreAacPreferencesRepository(dataStore())
        val order = listOf("help", "yes", "not_understood")

        // One write per test: a second rename of the file fails on Windows.
        repository.update { it.copy(readyPhraseOrder = order) }

        assertEquals(order, repository.preferences.first().readyPhraseOrder)
    }

    @Test
    fun `with no order chosen, none is stored and the app's order stays`() = runTest {
        val repository = DataStoreAacPreferencesRepository(dataStore())

        repository.update { it.copy(symbolSize = SymbolSize.LARGE, readyPhraseOrder = emptyList()) }

        assertEquals(AacPreferences(symbolSize = SymbolSize.LARGE), repository.preferences.first())
    }

    @Test
    fun `an unknown stored value falls back to the default instead of crashing`() = runTest {
        val dataStore = dataStore()
        // A value written by a newer app version.
        dataStore.edit { it[stringPreferencesKey("aac_symbol_size")] = "HUGE" }

        assertEquals(SymbolSize.SMALL, DataStoreAacPreferencesRepository(dataStore).preferences.first().symbolSize)
    }
}
