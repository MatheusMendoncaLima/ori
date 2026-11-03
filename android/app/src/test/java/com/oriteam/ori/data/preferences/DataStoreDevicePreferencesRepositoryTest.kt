package com.oriteam.ori.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.oriteam.ori.domain.preferences.AccountPreferences
import com.oriteam.ori.domain.preferences.ContentDensity
import com.oriteam.ori.domain.preferences.DevicePreferences
import com.oriteam.ori.domain.preferences.ListPresentation
import com.oriteam.ori.domain.preferences.MotionLevel
import com.oriteam.ori.domain.preferences.StartArea
import com.oriteam.ori.domain.preferences.TextScale
import com.oriteam.ori.domain.preferences.ThemeMode
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
class DataStoreDevicePreferencesRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun TestScope.dataStore(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = backgroundScope) { File(folder.root, "device.preferences_pb") }

    @Test
    fun `device choices start with the defaults and are kept`() = runTest {
        val repository = DataStoreDevicePreferencesRepository(dataStore())
        assertEquals(DevicePreferences(), repository.preferences.first())
        val chosen = DevicePreferences(ContentDensity.SPACIOUS, ListPresentation.SUMMARY, StartArea.COMMUNICATION)

        repository.update { chosen }

        assertEquals(chosen, repository.preferences.first())
    }

    @Test
    fun `the copy of the account choices is kept for the next start`() = runTest {
        val repository = DataStoreDevicePreferencesRepository(dataStore())
        val chosen = AccountPreferences(TextScale.LARGER, ThemeMode.DARK, isSimplified = true, motion = MotionLevel.OFF)

        repository.saveAccountCopy(chosen)

        assertEquals(chosen, repository.accountCopy.first())
    }

    @Test
    fun `an unknown stored value falls back to the default`() = runTest {
        val dataStore = dataStore()
        dataStore.edit { it[stringPreferencesKey("device_density")] = "HUGE" }

        assertEquals(
            ContentDensity.COMFORTABLE,
            DataStoreDevicePreferencesRepository(dataStore).preferences.first().density
        )
    }
}
