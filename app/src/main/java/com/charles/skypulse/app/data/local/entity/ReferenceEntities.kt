package com.charles.skypulse.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** OpenFlights airport, imported from bundled assets on first launch. */
@Entity(
    tableName = "airports",
    indices = [Index("iata"), Index("icao"), Index("lat"), Index("lon")],
)
data class AirportEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val city: String?,
    val country: String?,
    val iata: String?,
    val icao: String?,
    val lat: Double,
    val lon: Double,
    val altitudeFeet: Int?,
)

/** OpenFlights airline, imported from bundled assets on first launch. */
@Entity(
    tableName = "airlines",
    indices = [Index("iata"), Index("icao")],
)
data class AirlineEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val iata: String?,
    val icao: String?,
    val country: String?,
    val active: Boolean,
)
