package com.charles.skypulse.app.domain.model

/** OpenFlights airport record (normalized). */
data class Airport(
    val id: Int,
    val name: String,
    val city: String?,
    val country: String?,
    val iata: String?,
    val icao: String?,
    val latitude: Double,
    val longitude: Double,
    val altitudeFeet: Int?,
    /** Distance from user in km, populated for "near me" queries. */
    val distanceKm: Double? = null,
) {
    val code: String get() = icao?.takeIf { it.isNotBlank() } ?: iata.orEmpty()
}

/** OpenFlights airline record (normalized). */
data class Airline(
    val id: Int,
    val name: String,
    val iata: String?,
    val icao: String?,
    val country: String?,
    val active: Boolean,
)
