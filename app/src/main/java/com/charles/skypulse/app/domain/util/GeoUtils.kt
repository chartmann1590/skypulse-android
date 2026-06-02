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
    const val EARTH_RADIUS_NM = EARTH_RADIUS_KM / 1.852
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

    /**
     * Signed cross-track distance (NM) of point 3 from the great-circle path from point 1 to
     * point 2. |value| is how far the point lies off the direct route line.
     */
    fun crossTrackNm(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double,
        lat3: Double, lon3: Double,
    ): Double {
        val d13 = haversineNm(lat1, lon1, lat3, lon3) / EARTH_RADIUS_NM // angular (rad)
        val b13 = Math.toRadians(bearingDegrees(lat1, lon1, lat3, lon3))
        val b12 = Math.toRadians(bearingDegrees(lat1, lon1, lat2, lon2))
        return asin(sin(d13) * sin(b13 - b12)) * EARTH_RADIUS_NM
    }

    /** Along-track distance (NM): how far along the 1->2 path the projection of point 3 lies. */
    fun alongTrackNm(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double,
        lat3: Double, lon3: Double,
    ): Double {
        val d13 = haversineNm(lat1, lon1, lat3, lon3) / EARTH_RADIUS_NM
        val dxt = crossTrackNm(lat1, lon1, lat2, lon2, lat3, lon3) / EARTH_RADIUS_NM
        val ratio = (cos(d13) / cos(dxt)).coerceIn(-1.0, 1.0)
        return kotlin.math.acos(ratio) * EARTH_RADIUS_NM
    }

    /** Compass abbreviation (N, NE, …) for a heading in degrees. */
    fun compass(headingDegrees: Double): String {
        val dirs = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val idx = (((headingDegrees % 360) + 360) % 360 / 45.0).toInt() % 8
        return dirs[idx]
    }
}
