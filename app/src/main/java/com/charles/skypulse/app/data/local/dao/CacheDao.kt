package com.charles.skypulse.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.charles.skypulse.app.data.local.entity.CachedAircraftEntity

@Dao
interface CacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<CachedAircraftEntity>)

    @Query("DELETE FROM cached_aircraft")
    suspend fun clear()

    @Query("SELECT * FROM cached_aircraft")
    suspend fun getAll(): List<CachedAircraftEntity>

    @Query("SELECT MAX(cachedAtEpochMs) FROM cached_aircraft")
    suspend fun lastCachedAt(): Long?

    @Transaction
    suspend fun replaceAll(rows: List<CachedAircraftEntity>) {
        clear()
        insertAll(rows)
    }
}
