package com.charles.skypulse.app.data.repository

import com.charles.skypulse.app.data.local.dao.AirportDao
import com.charles.skypulse.app.data.remote.AdsbDbApi
import com.charles.skypulse.app.data.remote.Fr24FeedDataSource
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
 * Resolves a flight's real origin/destination. Primary source is the FR24 live feed (which
 * carries each aircraft's actual current route); falls back to adsbdb's scheduled route
 * (validated against the live position) when FR24 has nothing. Never throws.
 */
@Singleton
class RouteRepository @Inject constructor(
    private val adsbDbApi: AdsbDbApi,
    private val fr24: Fr24FeedDataSource,
    private val airportDao: AirportDao,
) {
    private val hits = ConcurrentHashMap<String, FlightRoute>()
    private val misses = ConcurrentHashMap.newKeySet<String>()

    /** Real route for a specific aircraft (for the detail sheet). FR24 first, adsbdb fallback. */
    suspend fun routeForAircraft(aircraft: Aircraft): FlightRoute? {
        val key = aircraft.hex?.lowercase()
            ?: aircraft.callsign?.trim()?.uppercase()?.ifEmpty { null }
            ?: return null
        hits[key]?.let { return it }
        if (key in misses) return null

        var route = fr24RouteFor(aircraft)
        if (route == null) {
            route = routeForCallsign(aircraft.callsign)
                ?.takeIf {
                    RouteEstimator.isConsistent(it, aircraft.latitude, aircraft.longitude, aircraft.onGround, aircraft.altitudeFeet)
                }
        }
        if (route != null) hits[key] = route else misses.add(key)
        return route
    }

    private suspend fun fr24RouteFor(aircraft: Aircraft): FlightRoute? {
        val lat = aircraft.latitude ?: return null
        val lon = aircraft.longitude ?: return null
        val hex = aircraft.hex?.lowercase()
        val cs = aircraft.callsign?.trim()?.uppercase()
        val match = fr24.flightsAround(lat, lon, FR24_MATCH_RADIUS_NM).firstOrNull {
            (hex != null && it.hex == hex) || (cs != null && it.callsign?.uppercase() == cs)
        } ?: return null
        return buildRoute(match.callsign ?: cs.orEmpty(), match.originIata, match.destinationIata)
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

    private companion object {
        const val FR24_MATCH_RADIUS_NM = 60.0
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
