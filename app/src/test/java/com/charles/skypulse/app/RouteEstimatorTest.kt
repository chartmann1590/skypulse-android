package com.charles.skypulse.app

import com.charles.skypulse.app.domain.model.FlightRoute
import com.charles.skypulse.app.domain.model.RoutePoint
import com.charles.skypulse.app.domain.util.GeoUtils
import com.charles.skypulse.app.domain.util.RouteEstimator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    // --- route consistency (rejects stale adsbdb routes) ---

    private val lga = RoutePoint("LGA", "KLGA", "LaGuardia", "New York", 40.7772, -73.8726, "United States")

    @Test
    fun groundedAtWrongAirport_isRejected() {
        // The real RPA4797 bug: parked at LGA but adsbdb says ORD->ALB.
        val onGroundAtLga = RouteEstimator.isConsistent(route, lga.latitude, lga.longitude, onGround = true, altitudeFeet = 0.0)
        assertFalse(onGroundAtLga)
    }

    @Test
    fun airborneNearDestination_isConsistent() {
        // ~30 NM from ORD (route destination), descending.
        val nearOrd = RouteEstimator.isConsistent(route, 41.55, -88.25, onGround = false, altitudeFeet = 9000.0)
        assertTrue(nearOrd)
    }

    @Test
    fun airborneOnCorridor_isConsistent() {
        // Short route ALB->BOS; the lat/lon midpoint is essentially on the great circle.
        val alb = RoutePoint("ALB", "KALB", "Albany Intl", "Albany", 42.7483, -73.8017, "United States")
        val bos = RoutePoint("BOS", "KBOS", "Logan Intl", "Boston", 42.3656, -71.0096, "United States")
        val shortRoute = FlightRoute("XYZ1", alb, bos, "Test Air")
        val mid = RouteEstimator.isConsistent(
            shortRoute, (alb.latitude + bos.latitude) / 2, (alb.longitude + bos.longitude) / 2,
            onGround = false, altitudeFeet = 20000.0,
        )
        assertTrue(mid)
    }

    @Test
    fun airborneFarOffCorridor_isRejected() {
        // Over Florida — nowhere near SFO->ORD.
        val off = RouteEstimator.isConsistent(route, 27.0, -81.0, onGround = false, altitudeFeet = 35000.0)
        assertFalse(off)
    }

    @Test
    fun groundedAtEndpoint_isConsistent() {
        val atDest = RouteEstimator.isConsistent(route, ord.latitude, ord.longitude, onGround = true, altitudeFeet = 0.0)
        assertTrue(atDest)
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
