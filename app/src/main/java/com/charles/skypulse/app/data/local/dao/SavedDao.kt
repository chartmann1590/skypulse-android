package com.charles.skypulse.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.charles.skypulse.app.data.local.entity.SavedAircraftEntity
import com.charles.skypulse.app.data.local.entity.SavedAirportEntity
import com.charles.skypulse.app.data.local.entity.SavedAreaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedDao {

    // --- Aircraft ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAircraft(entity: SavedAircraftEntity)

    @Query("DELETE FROM saved_aircraft WHERE id = :id")
    suspend fun deleteAircraft(id: String)

    @Query("SELECT * FROM saved_aircraft ORDER BY savedAtEpochMs DESC")
    fun observeAircraft(): Flow<List<SavedAircraftEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_aircraft WHERE id = :id)")
    fun isAircraftSaved(id: String): Flow<Boolean>

    // --- Airports ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAirport(entity: SavedAirportEntity)

    @Query("DELETE FROM saved_airports WHERE airportId = :id")
    suspend fun deleteAirport(id: Int)

    @Query("SELECT * FROM saved_airports ORDER BY savedAtEpochMs DESC")
    fun observeAirports(): Flow<List<SavedAirportEntity>>

    // --- Areas ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveArea(entity: SavedAreaEntity)

    @Query("DELETE FROM saved_areas WHERE id = :id")
    suspend fun deleteArea(id: Long)

    @Query("SELECT * FROM saved_areas ORDER BY savedAtEpochMs DESC")
    fun observeAreas(): Flow<List<SavedAreaEntity>>
}
