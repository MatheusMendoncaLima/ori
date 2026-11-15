package com.oriteam.ori.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.oriteam.ori.data.aac.AndroidSpeechSynthesizer
import com.oriteam.ori.data.aac.AndroidSpeechVolume
import com.oriteam.ori.data.account.ContentResolverFileWriter
import com.oriteam.ori.data.account.FirestoreAccountDeletion
import com.oriteam.ori.data.account.RoutedAccountDataExporter
import com.oriteam.ori.data.carelink.FirestoreCareLinkRepository
import com.oriteam.ori.data.carelink.FunctionsInvitationRepository
import com.oriteam.ori.data.launcher.AndroidHomeActivation
import com.oriteam.ori.data.launcher.AndroidHomeSystem
import com.oriteam.ori.data.launcher.AndroidInstalledApps
import com.oriteam.ori.data.launcher.AndroidLockScreenWallpaper
import com.oriteam.ori.data.launcher.AndroidWidgetHost
import com.oriteam.ori.data.launcher.DataStoreLauncherSettingsRepository
import com.oriteam.ori.data.launcher.DeviceWallpaperStore
import com.oriteam.ori.data.legal.ConsentStore
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
import com.oriteam.ori.domain.aac.SpeechVolume
import com.oriteam.ori.domain.aac.StarterVocabulary
import com.oriteam.ori.domain.account.AccountDataExporter
import com.oriteam.ori.domain.account.AccountDeletion
import com.oriteam.ori.domain.account.UserFileWriter
import com.oriteam.ori.domain.carelink.CareLinkRepository
import com.oriteam.ori.domain.carelink.InvitationRepository
import com.oriteam.ori.domain.launcher.HomeActivation
import com.oriteam.ori.domain.launcher.HomeSystem
import com.oriteam.ori.domain.launcher.HomeWidgetHost
import com.oriteam.ori.domain.launcher.InstalledAppsRepository
import com.oriteam.ori.domain.launcher.LauncherSettingsRepository
import com.oriteam.ori.domain.launcher.LockScreenWallpaper
import com.oriteam.ori.domain.launcher.WallpaperStore
import com.oriteam.ori.domain.legal.ConsentRepository
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
import com.oriteam.ori.ui.app.screens.aac.starterVocabularyOf
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

    @Binds
    @Singleton
    abstract fun bindConsentRepository(implementation: ConsentStore): ConsentRepository

    @Binds
    abstract fun bindLauncherSettingsRepository(
        implementation: DataStoreLauncherSettingsRepository
    ): LauncherSettingsRepository

    @Binds
    abstract fun bindInstalledAppsRepository(implementation: AndroidInstalledApps): InstalledAppsRepository

    @Binds
    abstract fun bindHomeActivation(implementation: AndroidHomeActivation): HomeActivation

    @Binds
    abstract fun bindWallpaperStore(implementation: DeviceWallpaperStore): WallpaperStore

    @Binds
    abstract fun bindHomeWidgetHost(implementation: AndroidWidgetHost): HomeWidgetHost

    @Binds
    @Singleton
    abstract fun bindHomeSystem(implementation: AndroidHomeSystem): HomeSystem

    @Binds
    abstract fun bindLockScreenWallpaper(implementation: AndroidLockScreenWallpaper): LockScreenWallpaper

    @Binds
    abstract fun bindAccountDataExporter(implementation: RoutedAccountDataExporter): AccountDataExporter

    @Binds
    abstract fun bindAccountDeletion(implementation: FirestoreAccountDeletion): AccountDeletion

    @Binds
    abstract fun bindUserFileWriter(implementation: ContentResolverFileWriter): UserFileWriter

    /** Not a singleton: each screen gets its own voice engine connection and closes it when it ends. */
    @Binds
    abstract fun bindSpeechSynthesizer(implementation: AndroidSpeechSynthesizer): SpeechSynthesizer

    @Binds
    abstract fun bindSpeechVolume(implementation: AndroidSpeechVolume): SpeechVolume

    companion object {
        @Provides
        @Singleton
        fun provideDisplayPreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            context.displayPreferences

        /** The suggested board in every language; the texts are read once, from the app itself. */
        @Provides
        @Singleton
        fun provideStarterVocabulary(@ApplicationContext context: Context): StarterVocabulary =
            starterVocabularyOf(context)

        @Provides
        @Singleton
        fun provideOriDatabase(@ApplicationContext context: Context): OriDatabase =
            Room.databaseBuilder(context, OriDatabase::class.java, "ori_local.db")
                .addMigrations(
                    OriDatabase.MIGRATION_1_2,
                    OriDatabase.migration2To3(context.filesDir),
                    OriDatabase.MIGRATION_3_4,
                    OriDatabase.MIGRATION_4_5,
                    OriDatabase.MIGRATION_5_6
                )
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
