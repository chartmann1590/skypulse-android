package com.charles.skypulse.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * adsbdb route response. On success: {"response":{"flightroute":{...}}}.
 * On a not-found callsign the API returns HTTP 404 (handled as a miss by the repository).
 */
@Serializable
data class AdsbDbResponse(
    val response: AdsbDbResponseBody? = null,
)

@Serializable
data class AdsbDbResponseBody(
    val flightroute: AdsbDbFlightRoute? = null,
)

@Serializable
data class AdsbDbFlightRoute(
    val callsign: String? = null,
    val airline: AdsbDbAirline? = null,
    val origin: AdsbDbAirport? = null,
    val destination: AdsbDbAirport? = null,
)

@Serializable
data class AdsbDbAirline(
    val name: String? = null,
    val icao: String? = null,
    val iata: String? = null,
    val country: String? = null,
)

@Serializable
data class AdsbDbAirport(
    @SerialName("iata_code") val iataCode: String? = null,
    @SerialName("icao_code") val icaoCode: String? = null,
    val name: String? = null,
    val municipality: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val elevation: Int? = null,
    @SerialName("country_name") val countryName: String? = null,
)
