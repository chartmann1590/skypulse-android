package com.charles.skypulse.app

import com.charles.skypulse.app.domain.model.FlightRoute
import com.charles.skypulse.app.domain.model.RoutePoint
import com.charles.skypulse.app.domain.util.GeoUtils
import com.charles.skypulse.app.domain.util.RouteEstimator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteEstimatorTest {

    // SFO -> ORD
    private val sfo = RoutePoint("SFO", "KSFO", "San Francisco Intl", "San Francisco", 37.6189, -122.3750, "United States")
    private val ord = RoutePoint("ORD", "KORD", "O'Hare Intl", "Chicago", 41.9786, -87.9048, "United States")
    private val route = FlightRoute("UAL1", sfo, ord, "United Airlines")

    @Test
    fun atOrigin_progressZero() {
        val p = RouteEstimator.estimate(route, sfo.latitude, sfo.longitude, 0.0)
        assertEquals(0.0, p.fractionComplete, 0.02)
    }

    @Test
    fun atDestination_progressOne() {
        val p = RouteEstimator.estimate(route, ord.latitude, ord.longitude, 450.0)
        assertEquals(1.0, p.fractionComplete, 0.02)
        assertEquals(0, p.etaMinutes) // basically arrived
    }

    @Test
    fun midpoint_progressAboutHalf() {
        val midLat = (sfo.latitude + ord.latitude) / 2
        val midLon = (sfo.longitude + ord.longitude) / 2
        val p = RouteEstimator.estimate(route, midLat, midLon, 450.0)
        assertTrue("progress was ${p.fractionComplete}", p.fractionComplete in 0.4..0.6)
        assertTrue(p.etaMinutes != null && p.etaMinutes!! > 0)
    }

    @Test
    fun nullSpeed_noEta() {
        val p = RouteEstimator.estimate(route, sfo.latitude, sfo.longitude, null)
        assertNull(p.etaMinutes)
    }

    @Test
    fun lowSpeed_noEta() {
        val p = RouteEstimator.estimate(route, sfo.latitude, sfo.longitude, 10.0)
        assertNull(p.etaMinutes)
    }

    @Test
    fun eta_matchesDistanceOverSpeed() {
        // Halfway, 450 kts. Remaining ~ half of total great-circle distance.
        val total = GeoUtils.haversineNm(sfo.latitude, sfo.longitude, ord.latitude, ord.longitude)
        val midLat = (sfo.latitude + ord.latitude) / 2
        val midLon = (sfo.longitude + ord.longitude) / 2
        val p = RouteEstimator.estimate(route, midLat, midLon, 450.0)
        val expectedEta = (p.remainingNm / 450.0 * 60.0)
        assertEquals(expectedEta, p.etaMinutes!!.toDouble(), 1.0)
        assertTrue(p.remainingNm < total)
    }
}
