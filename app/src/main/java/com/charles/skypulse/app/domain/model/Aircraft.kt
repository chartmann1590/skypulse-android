package com.charles.skypulse.app.domain.model

/** Where a given snapshot of aircraft came from. */
enum class DataSource { ADSB_LOL, OPENSKY, FR24, CACHE }

/**
 * Normalized aircraft model shared across the app, independent of the source API.
 * All physical quantities are stored in display-agnostic base units:
 * altitude in feet, speed in knots, vertical rate in feet/min, heading in degrees.
 * Every field except [id] is nullable because free ADS-B feeds are frequently partial.
 */
data class Aircraft(
    val id: String,
    val callsign: String?,
    val hex: String?,
    val latitude: Double?,
    val longitude: Double?,
    val altitudeFeet: Double?,
    val speedKnots: Double?,
    val headingDegrees: Double?,
    val verticalRate: Double?,
    val originCountry: String?,
    val lastSeenEpochSeconds: Long?,
    val source: DataSource,
    val typeCode: String? = null,
    val onGround: Boolean = false,
    /** Distance from the query/user point in nautical miles, when known. */
    val distanceNm: Double? = null,
) {
    val displayName: String
        get() = callsign?.trim()?.takeIf { it.isNotEmpty() }
            ?: hex?.uppercase()
            ?: id
}
