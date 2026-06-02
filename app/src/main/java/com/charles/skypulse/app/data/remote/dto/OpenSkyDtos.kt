package com.charles.skypulse.app.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/**
 * OpenSky /states/all response. Each state vector is a heterogeneous JSON array, so we
 * keep them as JsonElement lists and pull typed values by index defensively.
 *
 * Field order (see rest.html):
 * 0 icao24, 1 callsign, 2 origin_country, 3 time_position, 4 last_contact, 5 longitude,
 * 6 latitude, 7 baro_altitude(m), 8 on_ground, 9 velocity(m/s), 10 true_track,
 * 11 vertical_rate(m/s), 12 sensors, 13 geo_altitude(m), 14 squawk, ...
 */
@Serializable
data class OpenSkyResponse(
    val time: Long? = null,
    val states: List<List<JsonElement>>? = null,
)

object OpenSkyIndex {
    const val ICAO24 = 0
    const val CALLSIGN = 1
    const val ORIGIN_COUNTRY = 2
    const val LAST_CONTACT = 4
    const val LONGITUDE = 5
    const val LATITUDE = 6
    const val BARO_ALTITUDE = 7
    const val ON_GROUND = 8
    const val VELOCITY = 9
    const val TRUE_TRACK = 10
    const val VERTICAL_RATE = 11
    const val GEO_ALTITUDE = 13
}

fun List<JsonElement>.stringAt(i: Int): String? =
    (getOrNull(i) as? JsonPrimitive)?.takeIf { it.isString }?.content?.trim()?.ifEmpty { null }

fun List<JsonElement>.doubleAt(i: Int): Double? =
    (getOrNull(i) as? JsonPrimitive)?.doubleOrNull

fun List<JsonElement>.longAt(i: Int): Long? =
    (getOrNull(i) as? JsonPrimitive)?.longOrNull

fun List<JsonElement>.boolAt(i: Int): Boolean =
    (getOrNull(i) as? JsonPrimitive)?.booleanOrNull ?: false
