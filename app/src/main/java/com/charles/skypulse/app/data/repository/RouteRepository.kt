package com.charles.skypulse.app.data.repository

import com.charles.skypulse.app.data.remote.AdsbDbApi
import com.charles.skypulse.app.data.remote.dto.AdsbDbAirport
import com.charles.skypulse.app.domain.model.FlightRoute
import com.charles.skypulse.app.domain.model.RoutePoint
import kotlinx.coroutines.CancellationException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves callsign -> [FlightRoute] via the free adsbdb API. Routes are effectively
 * static for a given callsign, so results (hits and misses) are cached in memory to avoid
 * repeat calls. Never throws.
 */
@Singleton
class RouteRepository @Inject constructor(
    private val adsbDbApi: AdsbDbApi,
) {
    private val hits = ConcurrentHashMap<String, FlightRoute>()
    private val misses = ConcurrentHashMap.newKeySet<String>()

    suspend fun routeForCallsign(callsign: String?): FlightRoute? {
        val key = callsign?.trim()?.uppercase()?.ifEmpty { null } ?: return null
        hits[key]?.let { return it }
        if (key in misses) return null

        val route = try {
            adsbDbApi.getRoute(key).response?.flightroute?.let { fr ->
                val origin = fr.origin?.toPoint()
                val destination = fr.destination?.toPoint()
                if (origin != null && destination != null) {
                    FlightRoute(
                        callsign = key,
                        origin = origin,
                        destination = destination,
                        airlineName = fr.airline?.name,
                    )
                } else {
                    null
                }
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            null // 404 (unknown callsign) or network error -> treat as a miss
        }

        if (route != null) hits[key] = route else misses.add(key)
        return route
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
