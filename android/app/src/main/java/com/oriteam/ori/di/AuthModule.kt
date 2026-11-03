package com.oriteam.ori.di

import com.oriteam.ori.data.auth.FirebaseAuthRepository
import com.oriteam.ori.domain.auth.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Tells Hilt which implementation answers for the [AuthRepository] contract. */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(implementation: FirebaseAuthRepository): AuthRepository
}
