package com.charles.skypulse.app.data.remote

import kotlinx.serialization.json.JsonObject
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

/**
 * Flightradar24 live feed (unofficial, keyless). Returns every tracked aircraft inside a
 * bounding box together with its origin/destination airports — the data behind FR24's site.
 * Browser-like headers are required or the endpoint returns 403.
 */
interface Fr24Api {

    @Headers(
        "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
        "Accept: application/json",
        "Referer: https://www.flightradar24.com/",
    )
    @GET(ApiEndpoints.FR24_FEED_PATH)
    suspend fun feed(
        /** "latMax,latMin,lonMin,lonMax" (north,south,west,east). */
        @Query("bounds") bounds: String,
        @Query("faa") faa: Int = 1,
        @Query("adsb") adsb: Int = 1,
        @Query("mlat") mlat: Int = 1,
        @Query("flarm") flarm: Int = 1,
        @Query("air") air: Int = 1,
        @Query("gnd") gnd: Int = 1,
        @Query("vehicles") vehicles: Int = 0,
        @Query("estimated") estimated: Int = 1,
        @Query("maxage") maxage: Int = 14400,
        @Query("gliders") gliders: Int = 0,
        @Query("stats") stats: Int = 0,
    ): JsonObject
}
