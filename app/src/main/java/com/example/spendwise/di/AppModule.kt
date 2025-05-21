package com.example.spendwise.di

import android.content.Context
import com.example.spendwise.auth.AuthRepository
import com.example.spendwise.auth.AuthRepositoryImpl
import com.example.spendwise.data.repository.UserRepository
import com.example.spendwise.data.repository.SecureTokenStorage
import com.example.spendwise.data.repository.BiometricHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = Firebase.auth

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase = Firebase.database

    @Provides
    @Singleton
    fun provideSecureTokenStorage(
        @ApplicationContext context: Context
    ): SecureTokenStorage = SecureTokenStorage(context)

    @Provides
    @Singleton
    fun provideBiometricHelper(
        @ApplicationContext context: Context
    ): BiometricHelper = BiometricHelper(context)

    @Provides
    @Singleton
    fun provideAuthRepository(
        auth: FirebaseAuth,
        tokenStorage: SecureTokenStorage
    ): AuthRepository = AuthRepositoryImpl(auth, tokenStorage)

    @Provides
    @Singleton
    fun provideUserRepository(
        database: FirebaseDatabase,
        auth: FirebaseAuth
    ): UserRepository = UserRepository(database, auth)
} 