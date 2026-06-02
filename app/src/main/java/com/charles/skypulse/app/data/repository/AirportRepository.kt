package com.charles.skypulse.app.data.repository

import com.charles.skypulse.app.data.local.dao.AirlineDao
import com.charles.skypulse.app.data.local.dao.AirportDao
import com.charles.skypulse.app.data.local.entity.AirportEntity
import com.charles.skypulse.app.domain.model.Airline
import com.charles.skypulse.app.domain.model.Airport
import com.charles.skypulse.app.domain.util.GeoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AirportRepository @Inject constructor(
    private val airportDao: AirportDao,
    private val airlineDao: AirlineDao,
) {
    suspend fun search(query: String): List<Airport> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isEmpty()) emptyList()
        else airportDao.search(q.uppercase().takeIf { q.length <= 4 } ?: q).map { it.toDomain() }
    }

    /** Airports near a point, sorted by distance (km), using a bbox prefilter + Haversine. */
    suspend fun airportsNearMe(
        lat: Double,
        lon: Double,
        radiusKm: Double = 150.0,
        limit: Int = 10,
    ): List<Airport> = withContext(Dispatchers.IO) {
        val radiusNm = radiusKm / GeoUtils.KM_PER_NM
        val box = GeoUtils.boundingBox(lat, lon, radiusNm)
        airportDao.inBoundingBox(box.latMin, box.lonMin, box.latMax, box.lonMax)
            .map { e ->
                val d = GeoUtils.haversineKm(lat, lon, e.lat, e.lon)
                e.toDomain().copy(distanceKm = d)
            }
            .filter { (it.distanceKm ?: Double.MAX_VALUE) <= radiusKm }
            .sortedBy { it.distanceKm }
            .take(limit)
    }

    suspend fun byCode(code: String): Airport? = withContext(Dispatchers.IO) {
        airportDao.byCode(code.uppercase())?.toDomain()
    }

    /** Resolve an airline name from the ICAO prefix of a callsign (e.g. "UAL123" -> United). */
    suspend fun airlineForCallsign(callsign: String?): Airline? = withContext(Dispatchers.IO) {
        val prefix = callsign?.trim()?.takeIf { it.length >= 3 }?.substring(0, 3)?.uppercase()
            ?: return@withContext null
        airlineDao.byIcao(prefix)?.let {
            Airline(it.id, it.name, it.iata, it.icao, it.country, it.active)
        }
    }
}

private fun AirportEntity.toDomain() = Airport(
    id = id,
    name = name,
    city = city,
    country = country,
    iata = iata,
    icao = icao,
    latitude = lat,
    longitude = lon,
    altitudeFeet = altitudeFeet,
)
