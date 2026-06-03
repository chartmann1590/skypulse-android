package com.charles.skypulse.app.di

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides @Singleton
    fun provideAnalytics(@ApplicationContext context: Context): FirebaseAnalytics =
        FirebaseAnalytics.getInstance(context)

    @Provides @Singleton
    fun provideCrashlytics(): FirebaseCrashlytics = FirebaseCrashlytics.getInstance()

    @Provides @Singleton
    fun provideRemoteConfig(): FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    @Provides @Singleton
    fun provideMessaging(): FirebaseMessaging = FirebaseMessaging.getInstance()
}
