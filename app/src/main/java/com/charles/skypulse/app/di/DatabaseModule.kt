package com.charles.skypulse.app.di

import android.content.Context
import androidx.room.Room
import com.charles.skypulse.app.data.local.SkyPulseDatabase
import com.charles.skypulse.app.data.local.dao.AirlineDao
import com.charles.skypulse.app.data.local.dao.AirportDao
import com.charles.skypulse.app.data.local.dao.AlertDao
import com.charles.skypulse.app.data.local.dao.CacheDao
import com.charles.skypulse.app.data.local.dao.SavedDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SkyPulseDatabase =
        Room.databaseBuilder(context, SkyPulseDatabase::class.java, SkyPulseDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideAirportDao(db: SkyPulseDatabase): AirportDao = db.airportDao()
    @Provides fun provideAirlineDao(db: SkyPulseDatabase): AirlineDao = db.airlineDao()
    @Provides fun provideSavedDao(db: SkyPulseDatabase): SavedDao = db.savedDao()
    @Provides fun provideAlertDao(db: SkyPulseDatabase): AlertDao = db.alertDao()
    @Provides fun provideCacheDao(db: SkyPulseDatabase): CacheDao = db.cacheDao()
}
