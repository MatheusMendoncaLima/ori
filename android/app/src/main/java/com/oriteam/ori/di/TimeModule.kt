package com.oriteam.ori.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

/** Provides the clock, so "today" can be fixed in tests instead of read from the device. */
@Module
@InstallIn(SingletonComponent::class)
object TimeModule {
    @Provides
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
