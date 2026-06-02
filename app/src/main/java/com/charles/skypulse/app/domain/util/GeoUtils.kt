package com.charles.skypulse.app.domain.util

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Geospatial helpers used by repositories, nearby sorting, and alert evaluation. */
object GeoUtils {

    const val EARTH_RADIUS_KM = 6371.0088
    const val KM_PER_NM = 1.852
    const val KM_PER_MILE = 1.609344

    /** Great-circle distance in kilometres between two WGS-84 points. */
    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        return 2 * EARTH_RADIUS_KM * asin(min(1.0, sqrt(a)))
    }

    fun haversineNm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double =
        haversineKm(lat1, lon1, lat2, lon2) / KM_PER_NM

    /** Initial bearing in degrees (0..360) from point 1 to point 2. */
    fun bearingDegrees(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val dLon = Math.toRadians(lon2 - lon1)
        val y = sin(dLon) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLon)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    data class BoundingBox(
        val latMin: Double,
        val lonMin: Double,
        val latMax: Double,
        val lonMax: Double,
    )

    /**
     * Approximate bounding box for a radius (in nautical miles) around a centre point.
     * Used to build OpenSky bbox queries.
     */
    fun boundingBox(lat: Double, lon: Double, radiusNm: Double): BoundingBox {
        val radiusKm = radiusNm * KM_PER_NM
        val latDelta = Math.toDegrees(radiusKm / EARTH_RADIUS_KM)
        val cosLat = max(0.000001, cos(Math.toRadians(lat)))
        val lonDelta = Math.toDegrees(radiusKm / (EARTH_RADIUS_KM * cosLat))
        return BoundingBox(
            latMin = (lat - latDelta).coerceIn(-90.0, 90.0),
            lonMin = (lon - lonDelta).coerceIn(-180.0, 180.0),
            latMax = (lat + latDelta).coerceIn(-90.0, 90.0),
            lonMax = (lon + lonDelta).coerceIn(-180.0, 180.0),
        )
    }

    /** Compass abbreviation (N, NE, …) for a heading in degrees. */
    fun compass(headingDegrees: Double): String {
        val dirs = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val idx = (((headingDegrees % 360) + 360) % 360 / 45.0).toInt() % 8
        return dirs[idx]
    }
}
