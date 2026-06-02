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

    fun estimate(
        route: FlightRoute,
        currentLat: Double?,
        currentLon: Double?,
        groundSpeedKnots: Double?,
    ): RouteProgress {
        val o = route.origin
        val d = route.destination
        val total = GeoUtils.haversineNm(o.latitude, o.longitude, d.latitude, d.longitude)
        if (currentLat == null || currentLon == null || total <= 0.0) {
            return RouteProgress(fractionComplete = 0.0, remainingNm = total, etaMinutes = null)
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
