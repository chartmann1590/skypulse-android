package com.charles.skypulse.app.data.repository

import com.charles.skypulse.app.data.local.dao.AirportDao
import com.charles.skypulse.app.data.remote.AdsbDbApi
import com.charles.skypulse.app.data.remote.dto.AdsbDbAirport
import com.charles.skypulse.app.domain.model.Aircraft
import com.charles.skypulse.app.domain.model.FlightRoute
import com.charles.skypulse.app.domain.model.RoutePoint
import com.charles.skypulse.app.domain.util.RouteEstimator
import kotlinx.coroutines.CancellationException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves a flight's origin/destination from adsbdb's scheduled route for its callsign
 * (validated against the live position). Never throws.
 */
@Singleton
class RouteRepository @Inject constructor(
    private val adsbDbApi: AdsbDbApi,
    private val airportDao: AirportDao,
) {
    private val hits = ConcurrentHashMap<String, FlightRoute>()
    private val misses = ConcurrentHashMap.newKeySet<String>()

    /** Scheduled route for a specific aircraft (for the detail sheet), via adsbdb. */
    suspend fun routeForAircraft(aircraft: Aircraft): FlightRoute? {
        val key = aircraft.hex?.lowercase()
            ?: aircraft.callsign?.trim()?.uppercase()?.ifEmpty { null }
            ?: return null
        hits[key]?.let { return it }
        if (key in misses) return null

        val route = routeForCallsign(aircraft.callsign)
            ?.takeIf {
                RouteEstimator.isConsistent(it, aircraft.latitude, aircraft.longitude, aircraft.onGround, aircraft.altitudeFeet)
            }
        if (route != null) hits[key] = route else misses.add(key)
        return route
    }

    /** Builds a [FlightRoute] from IATA codes, looking up airport coordinates in Room. */
    suspend fun buildRoute(callsign: String, originIata: String?, destinationIata: String?): FlightRoute? {
        val origin = routePoint(originIata) ?: return null
        val destination = routePoint(destinationIata) ?: return null
        return FlightRoute(callsign = callsign, origin = origin, destination = destination, airlineName = null)
    }

    /** Resolves an IATA/ICAO code to a [RoutePoint]; coordinates come from the OpenFlights DB. */
    suspend fun routePoint(code: String?): RoutePoint? {
        val c = code?.trim()?.uppercase()?.ifEmpty { null } ?: return null
        val e = airportDao.byCode(c)
        return if (e != null) {
            RoutePoint(e.iata, e.icao, e.name, e.city, e.lat, e.lon, e.country)
        } else {
            // Unknown airport: show the code, coordinates unknown (progress/ETA skipped).
            RoutePoint(c, null, c, null, Double.NaN, Double.NaN, null)
        }
    }

    /** adsbdb scheduled route by callsign (fallback / also used to seed). */
    suspend fun routeForCallsign(callsign: String?): FlightRoute? {
        val key = callsign?.trim()?.uppercase()?.ifEmpty { null } ?: return null
        return try {
            adsbDbApi.getRoute(key).response?.flightroute?.let { fr ->
                val o = fr.origin?.toPoint()
                val d = fr.destination?.toPoint()
                if (o != null && d != null) FlightRoute(key, o, d, fr.airline?.name) else null
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            null
        }
    }
}

private fun AdsbDbAirport.toPoint(): RoutePoint? {
    val lat = latitude ?: return null
    val lon = longitude ?: return null
    return RoutePoint(
        iata = iataCode,
        icao = icaoCode,
        name = name,
        city = municipality,
        latitude = lat,
        longitude = lon,
        countryName = countryName,
    )
}
