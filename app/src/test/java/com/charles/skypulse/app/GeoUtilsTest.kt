package com.charles.skypulse.app

import com.charles.skypulse.app.domain.util.GeoUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoUtilsTest {

    @Test
    fun haversine_knownDistance_londonToParis() {
        // LHR (51.4700, -0.4543) to CDG (49.0097, 2.5479) ~ 348 km.
        val km = GeoUtils.haversineKm(51.4700, -0.4543, 49.0097, 2.5479)
        assertEquals(348.0, km, 10.0)
    }

    @Test
    fun haversine_samePoint_isZero() {
        assertEquals(0.0, GeoUtils.haversineKm(40.0, -73.0, 40.0, -73.0), 0.0001)
    }

    @Test
    fun nm_conversion_isConsistent() {
        val km = GeoUtils.haversineKm(0.0, 0.0, 0.0, 1.0)
        val nm = GeoUtils.haversineNm(0.0, 0.0, 0.0, 1.0)
        assertEquals(km / 1.852, nm, 0.0001)
    }

    @Test
    fun boundingBox_containsCenter_andHasWidth() {
        val box = GeoUtils.boundingBox(51.5, -0.12, 50.0)
        assertTrue(box.latMin < 51.5 && box.latMax > 51.5)
        assertTrue(box.lonMin < -0.12 && box.lonMax > -0.12)
    }

    @Test
    fun bearing_dueEast_isNinetyDegrees() {
        val b = GeoUtils.bearingDegrees(0.0, 0.0, 0.0, 1.0)
        assertEquals(90.0, b, 0.5)
    }

    @Test
    fun compass_cardinalDirections() {
        assertEquals("N", GeoUtils.compass(0.0))
        assertEquals("E", GeoUtils.compass(90.0))
        assertEquals("S", GeoUtils.compass(180.0))
        assertEquals("W", GeoUtils.compass(270.0))
        assertEquals("N", GeoUtils.compass(360.0))
    }
}
