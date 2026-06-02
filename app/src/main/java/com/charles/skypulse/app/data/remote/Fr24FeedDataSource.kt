package com.charles.skypulse.app.data.remote

import com.charles.skypulse.app.domain.util.GeoUtils
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import javax.inject.Inject
import javax.inject.Singleton

/** One aircraft from the FR24 feed, including its FR24-derived origin/destination. */
data class Fr24Flight(
    val hex: String?,
    val latitude: Double?,
    val longitude: Double?,
    val trackDeg: Double?,
    val altitudeFeet: Double?,
    val groundSpeedKnots: Double?,
    val verticalRate: Double?,
    val aircraftType: String?,
    val registration: String?,
    val originIata: String?,
    val destinationIata: String?,
    val flightNumber: String?,
    val callsign: String?,
    val onGround: Boolean,
    /** Epoch seconds of the last position report. */
    val timestampSeconds: Long?,
)

/**
 * Reads the FR24 feed for a bounding box and parses the positional arrays. Never throws —
 * returns an empty list on any failure so callers can fall back to other sources.
 */
@Singleton
class Fr24FeedDataSource @Inject constructor(
    private val api: Fr24Api,
) {
    suspend fun flightsAround(lat: Double, lon: Double, radiusNm: Double): List<Fr24Flight> {
        val box = GeoUtils.boundingBox(lat, lon, radiusNm)
        // FR24 bounds order: north,south,west,east.
        val bounds = "${box.latMax},${box.latMin},${box.lonMin},${box.lonMax}"
        return try {
            val obj = api.feed(bounds)
            obj.entries.mapNotNull { (key, value) ->
                if (key == "full_count" || key == "version" || key == "stats") return@mapNotNull null
                (value as? JsonArray)?.let { parse(it) }
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            emptyList()
        }
    }

    private fun parse(arr: JsonArray): Fr24Flight? {
        fun str(i: Int): String? =
            (arr.getOrNull(i) as? JsonPrimitive)?.takeIf { it.isString }?.content?.trim()?.ifEmpty { null }
        fun dbl(i: Int): Double? = (arr.getOrNull(i) as? JsonPrimitive)?.doubleOrNull
        val lat = dbl(IDX_LAT)
        val lon = dbl(IDX_LON)
        if (lat == null || lon == null) return null
        return Fr24Flight(
            hex = str(IDX_HEX)?.lowercase(),
            latitude = lat,
            longitude = lon,
            trackDeg = dbl(IDX_TRACK),
            altitudeFeet = dbl(IDX_ALT),
            groundSpeedKnots = dbl(IDX_SPD),
            verticalRate = dbl(IDX_VSPEED),
            aircraftType = str(IDX_TYPE),
            registration = str(IDX_REG),
            originIata = str(IDX_ORIGIN)?.uppercase(),
            destinationIata = str(IDX_DEST)?.uppercase(),
            flightNumber = str(IDX_FLIGHT),
            callsign = str(IDX_CALLSIGN),
            onGround = (dbl(IDX_ONGROUND) ?: 0.0) >= 1.0,
            timestampSeconds = dbl(IDX_TIMESTAMP)?.toLong(),
        )
    }

    private companion object {
        // FR24 feed array layout.
        const val IDX_HEX = 0
        const val IDX_LAT = 1
        const val IDX_LON = 2
        const val IDX_TRACK = 3
        const val IDX_ALT = 4
        const val IDX_SPD = 5
        const val IDX_TIMESTAMP = 10
        const val IDX_TYPE = 8
        const val IDX_REG = 9
        const val IDX_ORIGIN = 11
        const val IDX_DEST = 12
        const val IDX_FLIGHT = 13
        const val IDX_ONGROUND = 14
        const val IDX_VSPEED = 15
        const val IDX_CALLSIGN = 16
    }
}
