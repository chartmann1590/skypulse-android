package com.charles.skypulse.app.domain.model

/**
 * A snapshot of a flight shared via the "Share" button. Bundles the live [Aircraft] reading at
 * share time with its resolved [FlightRoute] and [RouteProgress] (when available). Used both when
 * writing a share to Firestore and when reconstructing one from a deep link.
 */
data class SharedFlight(
    val aircraft: Aircraft,
    val route: FlightRoute?,
    val progress: RouteProgress?,
)
