package com.charles.skypulse.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * ADSB.lol v2 response. Almost every per-aircraft field is optional because the open feed
 * is frequently partial — parse defensively. Unknown keys are ignored by the Json config.
 */
@Serializable
data class AdsbResponse(
    val ac: List<AdsbAircraftDto>? = null,
    val now: Long? = null,
    val total: Int? = null,
)

@Serializable
data class AdsbAircraftDto(
    val hex: String? = null,
    val flight: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    // alt_baro may be a number (feet) OR the string "ground". Keep raw and interpret.
    @SerialName("alt_baro") val altBaro: JsonElement? = null,
    val gs: Double? = null,
    val track: Double? = null,
    @SerialName("baro_rate") val baroRate: Double? = null,
    val seen: Double? = null,
    val t: String? = null,
    val dst: Double? = null,
    @SerialName("r") val registration: String? = null,
)

/** Interpreted barometric altitude. */
data class AltBaro(val feet: Double?, val onGround: Boolean)

/** Tolerant parse of the ADSB.lol `alt_baro` field (number-or-"ground"). */
fun parseAltBaro(element: JsonElement?): AltBaro {
    if (element == null) return AltBaro(null, false)
    val primitive = element as? JsonPrimitive ?: return AltBaro(null, false)
    if (primitive.isString && primitive.content.equals("ground", ignoreCase = true)) {
        return AltBaro(0.0, true)
    }
    return AltBaro(primitive.doubleOrNull ?: element.jsonPrimitive.content.toDoubleOrNull(), false)
}
