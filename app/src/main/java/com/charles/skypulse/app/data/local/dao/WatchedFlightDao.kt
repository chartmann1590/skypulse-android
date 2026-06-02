package com.charles.skypulse.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.charles.skypulse.app.data.local.entity.WatchedFlightStateEntity

@Dao
interface WatchedFlightDao {

    @Query("SELECT * FROM watched_flight_state WHERE id = :id")
    suspend fun get(id: String): WatchedFlightStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: WatchedFlightStateEntity)
}
