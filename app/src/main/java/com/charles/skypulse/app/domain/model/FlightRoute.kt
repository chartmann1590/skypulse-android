package com.charles.skypulse.app.domain.model

/** One end of a flight route (origin or destination airport). */
data class RoutePoint(
    val iata: String?,
    val icao: String?,
    val name: String?,
    val city: String?,
    val latitude: Double,
    val longitude: Double,
    val countryName: String?,
) {
    val code: String get() = iata?.takeIf { it.isNotBlank() } ?: icao.orEmpty()
}

/** A resolved flight route from adsbdb (free, no key). */
data class FlightRoute(
    val callsign: String,
    val origin: RoutePoint,
    val destination: RoutePoint,
    val airlineName: String?,
)

/** Estimated progress along a route, derived from the live position + groundspeed. */
data class RouteProgress(
    val fractionComplete: Double,
    val remainingNm: Double,
    val etaMinutes: Int?,
)
