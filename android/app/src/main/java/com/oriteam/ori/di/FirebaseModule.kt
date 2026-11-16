package com.oriteam.ori.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.oriteam.ori.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Local Firestore cache limit; the SDK evicts the least recently used documents above it. */
private const val FIRESTORE_CACHE_BYTES = 50L * 1024 * 1024

// Default ports of the Firebase Emulator Suite (see functions/firebase.json).
private const val AUTH_EMULATOR_PORT = 9099
private const val FIRESTORE_EMULATOR_PORT = 8080

/**
 * Creates the Firebase clients once. The `dev` flavor points every client to the local
 * Emulator Suite ([BuildConfig.FIREBASE_EMULATOR_HOST]); `staging` and `prod` use the
 * project in their own `google-services.json`.
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
    private val emulatorHost: String? = BuildConfig.FIREBASE_EMULATOR_HOST.takeIf { it.isNotBlank() }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance().apply {
        emulatorHost?.let { useEmulator(it, AUTH_EMULATOR_PORT) }
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance().apply {
        // Must run before the first read or write.
        emulatorHost?.let { useEmulator(it, FIRESTORE_EMULATOR_PORT) }
        firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(PersistentCacheSettings.newBuilder().setSizeBytes(FIRESTORE_CACHE_BYTES).build())
            .build()
    }
}
