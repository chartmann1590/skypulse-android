package com.charles.skypulse.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A saved aircraft (by hex/callsign) for quick access on the Saved screen. */
@Entity(tableName = "saved_aircraft")
data class SavedAircraftEntity(
    @PrimaryKey val id: String, // hex or callsign
    val callsign: String?,
    val hex: String?,
    val typeLabel: String?,
    val savedAtEpochMs: Long,
)

/** A saved airport. */
@Entity(tableName = "saved_airports")
data class SavedAirportEntity(
    @PrimaryKey val airportId: Int,
    val code: String,
    val name: String,
    val city: String?,
    val country: String?,
    val lat: Double,
    val lon: Double,
    val savedAtEpochMs: Long,
)

/** A saved search area (centre + radius). */
@Entity(tableName = "saved_areas")
data class SavedAreaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val lat: Double,
    val lon: Double,
    val radiusNm: Double,
    val savedAtEpochMs: Long,
)

/** Persisted local alert rule. */
@Entity(tableName = "alert_rules")
data class AlertRuleEntity(
    @PrimaryKey val type: String, // AlertType name — one rule per type matches the UI
    val enabled: Boolean,
    val radiusNm: Double,
    val callsign: String?,
    val altitudeThresholdFeet: Double,
    val anchorLat: Double?,
    val anchorLon: Double?,
)

/**
 * Tracks per-saved-flight state across worker runs so departure (on-ground -> airborne)
 * and landing-soon events can be detected and de-duplicated.
 */
@Entity(tableName = "watched_flight_state")
data class WatchedFlightStateEntity(
    @PrimaryKey val id: String, // saved aircraft id (hex or callsign)
    val lastOnGround: Boolean,
    val departedNotifiedAtMs: Long,
    val landingNotifiedAtMs: Long,
    val updatedAtMs: Long,
)

/** A cached aircraft snapshot row, used for offline/throttled responses. */
@Entity(tableName = "cached_aircraft")
data class CachedAircraftEntity(
    @PrimaryKey val id: String,
    val callsign: String?,
    val hex: String?,
    val lat: Double?,
    val lon: Double?,
    val altitudeFeet: Double?,
    val speedKnots: Double?,
    val headingDegrees: Double?,
    val verticalRate: Double?,
    val originCountry: String?,
    val lastSeenEpochSeconds: Long?,
    val typeCode: String?,
    val onGround: Boolean,
    val cachedAtEpochMs: Long,
)
