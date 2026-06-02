package com.charles.skypulse.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.charles.skypulse.app.data.local.entity.AirlineEntity
import com.charles.skypulse.app.data.local.entity.AirportEntity

@Dao
interface AirportDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(airports: List<AirportEntity>)

    @Query("SELECT COUNT(*) FROM airports")
    suspend fun count(): Int

    @Query(
        """
        SELECT * FROM airports
        WHERE name LIKE '%' || :q || '%'
           OR city LIKE '%' || :q || '%'
           OR country LIKE '%' || :q || '%'
           OR iata LIKE :q || '%'
           OR icao LIKE :q || '%'
        ORDER BY
           CASE WHEN iata = :q OR icao = :q THEN 0 ELSE 1 END,
           name
        LIMIT :limit
        """,
    )
    suspend fun search(q: String, limit: Int = 40): List<AirportEntity>

    /** Candidate airports inside a lat/lon bounding box (distance refined in Kotlin). */
    @Query(
        """
        SELECT * FROM airports
        WHERE lat BETWEEN :latMin AND :latMax
          AND lon BETWEEN :lonMin AND :lonMax
          AND (iata IS NOT NULL AND iata <> '')
        LIMIT :limit
        """,
    )
    suspend fun inBoundingBox(
        latMin: Double,
        lonMin: Double,
        latMax: Double,
        lonMax: Double,
        limit: Int = 200,
    ): List<AirportEntity>

    @Query("SELECT * FROM airports WHERE iata = :code OR icao = :code LIMIT 1")
    suspend fun byCode(code: String): AirportEntity?
}

@Dao
interface AirlineDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(airlines: List<AirlineEntity>)

    @Query("SELECT COUNT(*) FROM airlines")
    suspend fun count(): Int

    /** Look up an airline by the 3-letter ICAO prefix of a callsign (e.g. UAL -> United). */
    @Query("SELECT * FROM airlines WHERE icao = :icao AND icao <> '' LIMIT 1")
    suspend fun byIcao(icao: String): AirlineEntity?
}
