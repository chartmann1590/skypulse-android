package com.charles.skypulse.app.domain.model

/** The four local alert categories from the Alerts screen. */
enum class AlertType {
    AIRCRAFT_ENTERS_AREA,
    SPECIFIC_CALLSIGN,
    LOW_ALTITUDE_NEARBY,
    AIRPORT_ACTIVITY_NEARBY,
}

/**
 * A local-only alert rule. Evaluated on-device by [com.charles.skypulse.app.worker.AlertCheckWorker];
 * never leaves the phone.
 */
data class AlertRule(
    val id: Long = 0,
    val type: AlertType,
    val enabled: Boolean,
    /** Radius in nautical miles (AIRCRAFT_ENTERS_AREA / AIRPORT_ACTIVITY_NEARBY). */
    val radiusNm: Double = 15.0,
    /** Callsign to match (SPECIFIC_CALLSIGN). */
    val callsign: String? = null,
    /** Altitude threshold in feet (LOW_ALTITUDE_NEARBY). */
    val altitudeThresholdFeet: Double = 5000.0,
    /** Optional anchor location; falls back to live user location when null. */
    val anchorLat: Double? = null,
    val anchorLon: Double? = null,
)
