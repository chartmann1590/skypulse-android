package com.charles.skypulse.app.data.remote

import com.charles.skypulse.app.data.remote.dto.AdsbAircraftDto
import com.charles.skypulse.app.data.remote.dto.OpenSkyIndex
import com.charles.skypulse.app.data.remote.dto.boolAt
import com.charles.skypulse.app.data.remote.dto.doubleAt
import com.charles.skypulse.app.data.remote.dto.longAt
import com.charles.skypulse.app.data.remote.dto.parseAltBaro
import com.charles.skypulse.app.data.remote.dto.stringAt
import com.charles.skypulse.app.domain.model.Aircraft
import com.charles.skypulse.app.domain.model.DataSource
import com.charles.skypulse.app.domain.util.FetchResult
import com.charles.skypulse.app.domain.util.GeoUtils
import com.charles.skypulse.app.domain.util.UnitConverters
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches aircraft from the two free providers and maps each provider's raw shape into the
 * normalized [Aircraft] model. Network/parse failures are converted to [FetchResult.Error]
 * (never thrown) so the repository can fall back gracefully.
 */
@Singleton
class RemoteAircraftDataSource @Inject constructor(
    private val adsbApi: AdsbLolApi,
    private val openSkyApi: OpenSkyApi,
) {

    suspend fun fetchFromAdsbLol(lat: Double, lon: Double, radiusNm: Int): FetchResult {
        return try {
            val capped = radiusNm.coerceIn(1, ApiEndpoints.MAX_RADIUS_NM)
            val response = adsbApi.getByRadius(lat, lon, capped)
            val list = response.ac.orEmpty().mapNotNull { it.toAircraft() }
            if (list.isEmpty()) FetchResult.Empty
            else FetchResult.Success(list, DataSource.ADSB_LOL)
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            FetchResult.Error("ADSB.lol request failed: ${t.message}", t)
        }
    }

    suspend fun fetchFromOpenSky(lat: Double, lon: Double, radiusNm: Int): FetchResult {
        return try {
            val box = GeoUtils.boundingBox(lat, lon, radiusNm.toDouble())
            val response = openSkyApi.getStatesInBoundingBox(
                latMin = box.latMin, lonMin = box.lonMin,
                latMax = box.latMax, lonMax = box.lonMax,
            )
            val list = response.states.orEmpty().mapNotNull { it.toAircraft() }
            if (list.isEmpty()) FetchResult.Empty
            else FetchResult.Success(list, DataSource.OPENSKY)
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            FetchResult.Error("OpenSky request failed: ${t.message}", t)
        }
    }

    private fun AdsbAircraftDto.toAircraft(): Aircraft? {
        val id = hex?.trim()?.takeIf { it.isNotEmpty() }
            ?: flight?.trim()?.takeIf { it.isNotEmpty() }
            ?: return null
        val alt = parseAltBaro(altBaro)
        val lastSeen = seen?.let { (System.currentTimeMillis() / 1000) - it.toLong() }
        return Aircraft(
            id = id,
            callsign = flight?.trim()?.ifEmpty { null },
            hex = hex?.trim()?.lowercase(),
            latitude = lat,
            longitude = lon,
            altitudeFeet = alt.feet,
            speedKnots = gs,
            headingDegrees = track,
            verticalRate = baroRate,
            originCountry = null, // ADSB.lol does not provide origin country.
            lastSeenEpochSeconds = lastSeen,
            source = DataSource.ADSB_LOL,
            typeCode = t?.trim()?.ifEmpty { null },
            onGround = alt.onGround,
            distanceNm = dst,
        )
    }

    private fun List<kotlinx.serialization.json.JsonElement>.toAircraft(): Aircraft? {
        val icao = stringAt(OpenSkyIndex.ICAO24) ?: return null
        val baroM = doubleAt(OpenSkyIndex.BARO_ALTITUDE)
        val geoM = doubleAt(OpenSkyIndex.GEO_ALTITUDE)
        val altM = baroM ?: geoM
        val velocityMs = doubleAt(OpenSkyIndex.VELOCITY)
        val vRateMs = doubleAt(OpenSkyIndex.VERTICAL_RATE)
        return Aircraft(
            id = icao,
            callsign = stringAt(OpenSkyIndex.CALLSIGN),
            hex = icao.lowercase(),
            latitude = doubleAt(OpenSkyIndex.LATITUDE),
            longitude = doubleAt(OpenSkyIndex.LONGITUDE),
            altitudeFeet = altM?.let { UnitConverters.metersToFeet(it) },
            speedKnots = velocityMs?.let { UnitConverters.msToKnots(it) },
            headingDegrees = doubleAt(OpenSkyIndex.TRUE_TRACK),
            verticalRate = vRateMs?.let { UnitConverters.msToFpm(it) },
            originCountry = stringAt(OpenSkyIndex.ORIGIN_COUNTRY),
            lastSeenEpochSeconds = longAt(OpenSkyIndex.LAST_CONTACT),
            source = DataSource.OPENSKY,
            onGround = boolAt(OpenSkyIndex.ON_GROUND),
        )
    }
}
