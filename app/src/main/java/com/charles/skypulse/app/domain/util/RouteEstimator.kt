package com.charles.skypulse.app.domain.util

import com.charles.skypulse.app.domain.model.FlightRoute
import com.charles.skypulse.app.domain.model.RouteProgress
import kotlin.math.roundToInt

/**
 * Estimates progress and time-remaining along a [FlightRoute] from the live position and
 * groundspeed. These are ESTIMATES — free ADS-B feeds don't publish scheduled times.
 */
object RouteEstimator {

    /** Below this groundspeed (knots) an ETA is meaningless (taxiing / stationary). */
    private const val MIN_SPEED_FOR_ETA = 40.0

    // --- Route validation (free routes from adsbdb can be stale) ---
    private const val AT_AIRPORT_NM = 6.0          // "parked/at the surface of" an airport
    private const val NEAR_ENDPOINT_NM = 45.0      // close enough to an endpoint to trust
    // En-route flights follow airways (not exact great circles) and can be far off the direct
    // line on long routes, so keep this generous; the precise rejection of stale routes comes
    // from the grounded/low rule below.
    private const val MAX_CROSS_TRACK_NM = 110.0   // max off-corridor distance when en route
    private const val ENDPOINT_MARGIN_NM = 60.0    // allowed overshoot past either endpoint
    private const val GROUND_ALT_FT = 4000.0

    /**
     * True if the aircraft's live position is physically consistent with actually flying [route].
     * Rejects stale/incorrect routes (e.g. a plane parked at LGA whose route DB still says
     * ORD->ALB): a grounded/low aircraft must be AT an endpoint, and an airborne one must be
     * near an endpoint or within the route corridor.
     */
    fun isConsistent(
        route: FlightRoute,
        lat: Double?,
        lon: Double?,
        onGround: Boolean,
        altitudeFeet: Double?,
    ): Boolean {
        if (lat == null || lon == null) return true // can't validate; don't block
        val o = route.origin
        val d = route.destination
        val distO = GeoUtils.haversineNm(lat, lon, o.latitude, o.longitude)
        val distD = GeoUtils.haversineNm(lat, lon, d.latitude, d.longitude)

        // Low/grounded: must be physically at one of the route's airports.
        if (onGround || (altitudeFeet != null && altitudeFeet < GROUND_ALT_FT)) {
            return distO <= AT_AIRPORT_NM || distD <= AT_AIRPORT_NM
        }
        // Airborne: near an endpoint, or within the great-circle corridor between them.
        if (distO <= NEAR_ENDPOINT_NM || distD <= NEAR_ENDPOINT_NM) return true
        val routeLen = GeoUtils.haversineNm(o.latitude, o.longitude, d.latitude, d.longitude)
        val xt = kotlin.math.abs(
            GeoUtils.crossTrackNm(o.latitude, o.longitude, d.latitude, d.longitude, lat, lon),
        )
        val at = GeoUtils.alongTrackNm(o.latitude, o.longitude, d.latitude, d.longitude, lat, lon)
        return xt <= MAX_CROSS_TRACK_NM && at in -ENDPOINT_MARGIN_NM..(routeLen + ENDPOINT_MARGIN_NM)
    }

    fun estimate(
        route: FlightRoute,
        currentLat: Double?,
        currentLon: Double?,
        groundSpeedKnots: Double?,
    ): RouteProgress {
        val o = route.origin
        val d = route.destination
        val total = GeoUtils.haversineNm(o.latitude, o.longitude, d.latitude, d.longitude)
        if (currentLat == null || currentLon == null || total.isNaN() || total <= 0.0) {
            return RouteProgress(fractionComplete = 0.0, remainingNm = if (total.isNaN()) 0.0 else total, etaMinutes = null)
        }
        val remaining = GeoUtils.haversineNm(currentLat, currentLon, d.latitude, d.longitude)
            .coerceIn(0.0, total)
        val fraction = ((total - remaining) / total).coerceIn(0.0, 1.0)
        val eta = if (groundSpeedKnots != null && groundSpeedKnots >= MIN_SPEED_FOR_ETA) {
            (remaining / groundSpeedKnots * 60.0).roundToInt()
        } else {
            null
        }
        return RouteProgress(fractionComplete = fraction, remainingNm = remaining, etaMinutes = eta)
    }
}
