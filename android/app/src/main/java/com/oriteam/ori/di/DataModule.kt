package com.oriteam.ori.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.oriteam.ori.data.aac.AndroidSpeechSynthesizer
import com.oriteam.ori.data.carelink.FirestoreCareLinkRepository
import com.oriteam.ori.data.carelink.FunctionsInvitationRepository
import com.oriteam.ori.data.local.OriDatabase
import com.oriteam.ori.data.local.RoomAacUsageRepository
import com.oriteam.ori.data.local.RoomLocalDataStore
import com.oriteam.ori.data.local.RoutedAacBoardRepository
import com.oriteam.ori.data.local.RoutedRoutineRepository
import com.oriteam.ori.data.local.RoutedRoutineSeriesRepository
import com.oriteam.ori.data.local.RoutedSavedMessageRepository
import com.oriteam.ori.data.photos.DevicePhotoStore
import com.oriteam.ori.data.photos.FirestoreCloudPhotoRepository
import com.oriteam.ori.data.preferences.DataStoreAacPreferencesRepository
import com.oriteam.ori.data.preferences.DataStoreDevicePreferencesRepository
import com.oriteam.ori.data.preferences.FirestoreAccountPreferencesRepository
import com.oriteam.ori.data.profile.DataStoreDeviceProfileRepository
import com.oriteam.ori.data.profile.DeviceProfilePhotoStore
import com.oriteam.ori.data.profile.FirestoreProfileRepository
import com.oriteam.ori.data.routine.SharedReadingRoutineRepository
import com.oriteam.ori.data.routine.SharedReadingSeriesRepository
import com.oriteam.ori.domain.aac.AacBoardRepository
import com.oriteam.ori.domain.aac.AacUsageRepository
import com.oriteam.ori.domain.aac.SavedMessageRepository
import com.oriteam.ori.domain.aac.SpeechSynthesizer
import com.oriteam.ori.domain.carelink.CareLinkRepository
import com.oriteam.ori.domain.carelink.InvitationRepository
import com.oriteam.ori.domain.local.LocalDataStore
import com.oriteam.ori.domain.photos.CloudPhotoRepository
import com.oriteam.ori.domain.photos.PhotoStore
import com.oriteam.ori.domain.preferences.AacPreferencesRepository
import com.oriteam.ori.domain.preferences.AccountPreferencesRepository
import com.oriteam.ori.domain.preferences.DevicePreferencesRepository
import com.oriteam.ori.domain.profile.DeviceProfileRepository
import com.oriteam.ori.domain.profile.ProfilePhotoStore
import com.oriteam.ori.domain.profile.ProfileRepository
import com.oriteam.ori.domain.routine.RepeatingRoutineRepository
import com.oriteam.ori.domain.routine.RoutineRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

private val Context.displayPreferences: DataStore<Preferences> by preferencesDataStore(name = "display_preferences")

/** Tells Hilt which implementation answers for each data contract of the domain. */
@Suppress("TooManyFunctions") // One binding per data contract; they belong in one place.
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindProfileRepository(implementation: FirestoreProfileRepository): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindDeviceProfileRepository(implementation: DataStoreDeviceProfileRepository): DeviceProfileRepository

    @Binds
    @Singleton
    abstract fun bindProfilePhotoStore(implementation: DeviceProfilePhotoStore): ProfilePhotoStore

    @Binds
    @Singleton
    abstract fun bindCareLinkRepository(implementation: FirestoreCareLinkRepository): CareLinkRepository

    @Binds
    @Singleton
    abstract fun bindInvitationRepository(implementation: FunctionsInvitationRepository): InvitationRepository

    @Binds
    @Singleton
    abstract fun bindAacPreferencesRepository(
        implementation: DataStoreAacPreferencesRepository
    ): AacPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindAacBoardRepository(implementation: RoutedAacBoardRepository): AacBoardRepository

    @Binds
    @Singleton
    abstract fun bindSavedMessageRepository(implementation: RoutedSavedMessageRepository): SavedMessageRepository

    @Binds
    @Singleton
    abstract fun bindLocalDataTransfer(implementation: RoomLocalDataStore): LocalDataStore

    @Binds
    @Singleton
    abstract fun bindAacUsageRepository(implementation: RoomAacUsageRepository): AacUsageRepository

    @Binds
    @Singleton
    abstract fun bindCloudPhotoRepository(implementation: FirestoreCloudPhotoRepository): CloudPhotoRepository

    @Binds
    @Singleton
    abstract fun bindPhotoStore(implementation: DevicePhotoStore): PhotoStore

    @Binds
    @Singleton
    abstract fun bindAccountPreferencesRepository(
        implementation: FirestoreAccountPreferencesRepository
    ): AccountPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindDevicePreferencesRepository(
        implementation: DataStoreDevicePreferencesRepository
    ): DevicePreferencesRepository

    /** Not a singleton: each screen gets its own voice engine connection and closes it when it ends. */
    @Binds
    abstract fun bindSpeechSynthesizer(implementation: AndroidSpeechSynthesizer): SpeechSynthesizer

    companion object {
        @Provides
        @Singleton
        fun provideDisplayPreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            context.displayPreferences

        @Provides
        @Singleton
        fun provideOriDatabase(@ApplicationContext context: Context): OriDatabase =
            Room.databaseBuilder(context, OriDatabase::class.java, "ori_local.db")
                .addMigrations(OriDatabase.MIGRATION_1_2, OriDatabase.migration2To3(context.filesDir))
                .build()

        /**
         * Stored activities plus the repeating ones: what every screen sees as the routine. The screens
         * share the readings, which run on a scope that lives as long as the app and stop on their own
         * when no screen looks at them.
         */
        @Provides
        @Singleton
        fun provideRoutineRepository(
            tasks: RoutedRoutineRepository,
            series: RoutedRoutineSeriesRepository
        ): RoutineRepository {
            val readings = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val repeating = RepeatingRoutineRepository(tasks, SharedReadingSeriesRepository(series, readings))
            return SharedReadingRoutineRepository(repeating, readings)
        }
    }
}
