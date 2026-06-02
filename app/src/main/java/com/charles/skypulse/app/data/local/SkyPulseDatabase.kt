package com.charles.skypulse.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.charles.skypulse.app.data.local.dao.AirlineDao
import com.charles.skypulse.app.data.local.dao.AirportDao
import com.charles.skypulse.app.data.local.dao.AlertDao
import com.charles.skypulse.app.data.local.dao.CacheDao
import com.charles.skypulse.app.data.local.dao.SavedDao
import com.charles.skypulse.app.data.local.dao.WatchedFlightDao
import com.charles.skypulse.app.data.local.entity.AirlineEntity
import com.charles.skypulse.app.data.local.entity.AirportEntity
import com.charles.skypulse.app.data.local.entity.AlertRuleEntity
import com.charles.skypulse.app.data.local.entity.CachedAircraftEntity
import com.charles.skypulse.app.data.local.entity.SavedAircraftEntity
import com.charles.skypulse.app.data.local.entity.SavedAirportEntity
import com.charles.skypulse.app.data.local.entity.SavedAreaEntity
import com.charles.skypulse.app.data.local.entity.WatchedFlightStateEntity

@Database(
    entities = [
        AirportEntity::class,
        AirlineEntity::class,
        SavedAircraftEntity::class,
        SavedAirportEntity::class,
        SavedAreaEntity::class,
        AlertRuleEntity::class,
        CachedAircraftEntity::class,
        WatchedFlightStateEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class SkyPulseDatabase : RoomDatabase() {
    abstract fun airportDao(): AirportDao
    abstract fun airlineDao(): AirlineDao
    abstract fun savedDao(): SavedDao
    abstract fun alertDao(): AlertDao
    abstract fun cacheDao(): CacheDao
    abstract fun watchedFlightDao(): WatchedFlightDao

    companion object {
        const val NAME = "skypulse.db"
    }
}
