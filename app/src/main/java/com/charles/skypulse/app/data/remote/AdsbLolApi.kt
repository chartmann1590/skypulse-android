package com.charles.skypulse.app.data.remote

import com.charles.skypulse.app.data.remote.dto.AdsbResponse
import retrofit2.http.GET
import retrofit2.http.Path

/** ADSB.lol public REST API (no key/login). Paths centralized in [ApiEndpoints]. */
interface AdsbLolApi {

    @GET("v2/lat/{lat}/lon/{lon}/dist/{dist}")
    suspend fun getByRadius(
        @Path("lat") lat: Double,
        @Path("lon") lon: Double,
        @Path("dist") distNm: Int,
    ): AdsbResponse

    /** Locate a specific aircraft anywhere by ICAO hex. */
    @GET("v2/hex/{hex}")
    suspend fun getByHex(@Path("hex") hex: String): AdsbResponse

    /** Locate a specific aircraft anywhere by callsign. */
    @GET("v2/callsign/{callsign}")
    suspend fun getByCallsign(@Path("callsign") callsign: String): AdsbResponse
}
